package app

import app.application.usecase.*
import app.domain.repository.port.ServerApiPort
import app.domain.user.repository.UserRepository
import app.infrastructure.Logger
import app.infrastructure.api.ServerApiAdapter
import app.infrastructure.cli.*
import app.infrastructure.config.FileHelper.toPath
import app.infrastructure.config.FileUserRepository
import app.infrastructure.config.PasswordHelper
import app.infrastructure.event.InMemoryEventDispatcher
import app.infrastructure.extractor.DefaultServiceFactory
import app.infrastructure.git.JGitRepositoryAdapter
import app.infrastructure.persistence.*
import app.infrastructure.ui.ConsoleUi
import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import java.nio.file.Files
import java.nio.file.Path
import java.security.GeneralSecurityException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

fun main(argv: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { _, e: Throwable ->
        Logger.error(e, "Uncaught exception")
    }
    if (BuildConfig.ENV != "production") {
        disableSslChecks()
    }
    Main(argv)
}

/**
 * Ponto de entrada da aplicação.
 * Responsável pela composição (injeção de dependências manual)
 * e bootstrap de todas as camadas.
 */
class Main(argv: Array<String>) {
    // === Composição de dependências (Manual DI) ===
    // Infrastructure layer instances
    private val userRepository: UserRepository = FileUserRepository()
    private val apiPort: ServerApiPort = ServerApiAdapter()
    private val gitAdapter = JGitRepositoryAdapter()
    private val serviceFactory = DefaultServiceFactory()

    init {
        Logger.uuid = (userRepository as FileUserRepository).getUuid()
        Logger.info(Logger.Events.START) { "App started" }

        val options = Options()
        val commandAdd = CommandAdd()
        val commandConfig = CommandConfig()
        val commandList = CommandList()
        val commandRemove = CommandRemove()
        val jc: JCommander = JCommander.newBuilder()
            .programName("sourcerer")
            .addObject(options)
            .addCommand(commandAdd.name, commandAdd)
            .addCommand(commandConfig.name, commandConfig)
            .addCommand(commandList.name, commandList)
            .addCommand(commandRemove.name, commandRemove)
            .build()

        try {
            jc.parse(*argv)

            if (options.help) {
                showHelp(jc)
            } else if (options.setup) {
                doSetup()
            } else when (jc.parsedCommand) {
                commandAdd.name -> doAdd(commandAdd)
                commandConfig.name -> doConfig(commandConfig)
                commandList.name -> doList()
                commandRemove.name -> doRemove(commandRemove)
                else -> startUi()
            }
        } catch (e: MissingCommandException) {
            Logger.warn { "No such command: ${e.unknownCommand}" }
        }

        Logger.info(Logger.Events.EXIT) { "App finished" }
    }

    // === UI Flow ===

    private fun startUi() {
        ConsoleUi(
            apiPort = apiPort,
            userRepository = userRepository,
            gitValidator = gitAdapter,
            hashRepositoryUseCaseFactory = { createHashRepositoryUseCase() }
        )
    }

    // === CLI Commands ===

    private fun doAdd(commandAdd: CommandAdd) {
        val addUseCase = AddRepositoryUseCase(userRepository, gitAdapter)
        commandAdd.paths.forEach { pathStr ->
            val path = pathStr.toPath()
            if (commandAdd.recursive) {
                Files.walk(path)
                    .filter { p -> gitAdapter.isValidGitRepo(p.toString()) }
                    .forEach { p -> processPath(p, commandAdd.hashAll, addUseCase) }
            } else {
                processPath(path, commandAdd.hashAll, addUseCase)
            }
        }
    }

    private fun processPath(path: Path, hashAll: Boolean, addUseCase: AddRepositoryUseCase) {
        val success = addUseCase.execute(path.toString(), hashAll)
        if (success) {
            Logger.print("Added git repository at $path.")
            Logger.info(Logger.Events.CONFIG_CHANGED) { "Config changed" }
        } else {
            Logger.warn { "No valid git repository found at specified path $path" }
        }
    }

    private fun doConfig(commandOptions: CommandConfig) {
        if (commandOptions.pair.size < 2) {
            Logger.warn { "Config requires KEY VALUE pair" }
            return
        }
        val (key, value) = commandOptions.pair

        if (!arrayListOf("username", "password").contains(key)) {
            Logger.warn { "No such key $key" }
            return
        }

        val aggregate = userRepository.load()
        val currentCredentials = aggregate.credentials
        val currentUsername = currentCredentials?.username ?: ""
        val currentPasswordHash = currentCredentials?.passwordHash ?: ""

        when (key) {
            "username" -> {
                val pwHash = currentPasswordHash.ifEmpty { PasswordHelper.hashPassword("") }
                if (pwHash.isNotEmpty()) {
                    aggregate.defineCredentials(
                        app.domain.user.valueobject.Credentials(value, pwHash)
                    )
                }
            }
            "password" -> {
                val uname = currentUsername.ifEmpty { "user" }
                aggregate.defineCredentials(
                    app.domain.user.valueobject.Credentials(uname, PasswordHelper.hashPassword(value))
                )
            }
        }

        userRepository.save(aggregate)
        Logger.info(Logger.Events.CONFIG_CHANGED) { "Config changed" }
    }

    private fun doList() {
        val useCase = ListRepositoriesUseCase(userRepository)
        val repos = useCase.execute()
        if (repos.isNotEmpty()) {
            Logger.print("Tracked repositories:", indentLine = true)
            repos.forEach { Logger.print(it) }
        } else {
            Logger.print("No tracked repositories", indentLine = true)
        }
    }

    private fun doRemove(commandRemove: CommandRemove) {
        val path = commandRemove.path
        if (path != null) {
            val useCase = RemoveRepositoryUseCase(userRepository)
            useCase.execute(path)
            Logger.print("Repository removed from tracking list.")
            Logger.info(Logger.Events.CONFIG_CHANGED) { "Config changed" }
        } else {
            Logger.print("Repository not found in tracking list.")
        }
    }

    private fun doSetup() {
        val aggregate = userRepository.load()
        if (!aggregate.isFirstLaunch()) {
            Logger.print("Are you sure that you want to setup Sourcerer again? (y/N)")
            val answer = readLine() ?: ""
            if (answer.toLowerCase() != "y") return
            userRepository.resetAndSave()
        }
        startUi()
    }

    private fun showHelp(jc: JCommander) {
        Logger.print("Sourcerer hashes your git repositories into intelligent " +
            "engineering profiles.")
        Logger.print("If you don't have an account, please, proceed to " +
            "https://sourcerer.io/join")
        Logger.print("More info at https://sourcerer.io and " +
            "https://github.com/sourcerer-io")
        jc.usage()
    }

    // === Factory Methods (Composição) ===

    /**
     * Cria uma instância do HashRepositoryUseCase com todas as dependências injetadas.
     * Cada execução cria uma nova instância para evitar estado compartilhado.
     */
    private fun createHashRepositoryUseCase(): HashRepositoryUseCase {
        return HashRepositoryUseCase(
            gitPort = gitAdapter,
            apiPort = apiPort,
            repoRepository = ApiRepoRepository(apiPort),
            commitRepository = ApiCommitRepository(apiPort),
            authorRepository = ApiAuthorRepository(apiPort),
            factRepository = ApiFactRepository(apiPort),
            authorDistanceRepository = ApiAuthorDistanceRepository(apiPort),
            serviceFactory = serviceFactory,
            eventDispatcher = InMemoryEventDispatcher(),
            commitHasherEnabled = BuildConfig.COMMIT_HASHER_ENABLED,
            factHasherEnabled = BuildConfig.FACT_HASHER_ENABLED,
            longevityEnabled = BuildConfig.LONGEVITY_ENABLED,
            metaHasherEnabled = BuildConfig.META_HASHER_ENABLED,
            distancesEnabled = BuildConfig.DISTANCES_ENABLED
        )
    }
}

fun disableSslChecks() {
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
    })

    try {
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
    } catch (e: GeneralSecurityException) {}

    HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
}
