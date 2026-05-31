package app.infrastructure.config

import app.domain.user.aggregate.UserAggregate
import app.domain.user.entity.User
import app.domain.user.repository.UserRepository
import app.domain.user.valueobject.Credentials
import app.domain.user.valueobject.LocalRepo
import app.infrastructure.Logger
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.NoSuchFileException
import java.util.UUID

/**
 * Implementação de UserRepository baseada em arquivo YAML.
 * Persistência local de configurações do usuário.
 */
class FileUserRepository : UserRepository {

    private val CONFIG_FILE_NAME = "config.yaml"
    private val mapper = createMapper()

    private fun createMapper(): ObjectMapper {
        return ObjectMapper(YAMLFactory())
            .setVisibility(PropertyAccessor.ALL, Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
            .registerModule(KotlinModule())
    }

    override fun load(): UserAggregate {
        val configData = loadConfigFromFile()
        val user = User(uuid = configData.uuid)
        val aggregate = UserAggregate(user)

        if (configData.username.isNotEmpty() && configData.password.isNotEmpty()) {
            aggregate.defineCredentials(
                Credentials(configData.username, configData.password)
            )
        }

        configData.localRepos.forEach { repoPath ->
            aggregate.addLocalRepo(LocalRepo(path = repoPath))
        }

        return aggregate
    }

    override fun save(aggregate: UserAggregate) {
        val configData = ConfigData(
            uuid = aggregate.user.uuid.ifEmpty {
                UUID.randomUUID().toString()
            },
            username = aggregate.credentials?.username ?: "",
            password = aggregate.credentials?.passwordHash ?: "",
            localRepos = aggregate.getLocalRepos().map { it.path }.toMutableSet()
        )
        saveConfigToFile(configData)
    }

    override fun resetAndSave() {
        saveConfigToFile(ConfigData())
    }

    fun getUuid(): String {
        val config = loadConfigFromFile()
        if (config.uuid.isEmpty()) {
            config.uuid = UUID.randomUUID().toString()
            saveConfigToFile(config)
        }
        return config.uuid
    }

    private fun loadConfigFromFile(): ConfigData {
        var loadConfig = ConfigData()
        try {
            loadConfig = Files.newBufferedReader(
                FileHelper.getPath(CONFIG_FILE_NAME)
            ).use {
                mapper.readValue(it, ConfigData::class.java)
            }
        } catch (e: IOException) {
            if (e is NoSuchFileException) {
                Logger.warn { "No config file found" }
            } else {
                Logger.error(e, "Cannot access config file")
            }
        } catch (e: SecurityException) {
            Logger.error(e, "Cannot access config file")
        } catch (e: InvalidPathException) {
            Logger.error(e, "Cannot access config file")
        } catch (e: JsonParseException) {
            Logger.error(e, "Cannot parse config file")
        } catch (e: JsonMappingException) {
            Logger.error(e, "Cannot parse config file")
        } catch (e: IllegalStateException) {
            Logger.error(e, "Cannot parse config file")
        }
        return loadConfig
    }

    private fun saveConfigToFile(config: ConfigData) {
        try {
            Files.newBufferedWriter(FileHelper.getPath(CONFIG_FILE_NAME)).use {
                mapper.writeValue(it, config)
            }
        } catch (e: IOException) {
            Logger.error(e, "Cannot save config file")
        } catch (e: SecurityException) {
            Logger.error(e, "Cannot save config file")
        } catch (e: InvalidPathException) {
            Logger.error(e, "Cannot save config file")
        } catch (e: JsonParseException) {
            Logger.error(e, "Cannot parse config file")
        } catch (e: JsonMappingException) {
            Logger.error(e, "Cannot parse config file")
        } catch (e: IllegalStateException) {
            Logger.error(e, "Cannot parse config file")
        }
    }
}

/**
 * Modelo de persistência para configuração (entidade focada em persistência).
 * Separada da entidade de domínio (complexidade acidental).
 */
data class ConfigData(
    var uuid: String = "",
    var username: String = "",
    var password: String = "",
    var localRepos: MutableSet<String> = mutableSetOf()
)
