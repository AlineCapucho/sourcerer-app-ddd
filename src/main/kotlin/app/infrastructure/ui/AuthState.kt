package app.infrastructure.ui

import app.BuildConfig
import app.application.usecase.AuthResult
import app.application.usecase.AuthenticateUserUseCase
import app.infrastructure.Logger
import app.infrastructure.config.PasswordHelper

/**
 * Authorization console UI state.
 */
class AuthState(private val context: ConsoleUi) : ConsoleState {
    var username = ""
    var password = ""
    var retry = true
    var authorized = false

    override fun doAction() {
        val aggregate = context.userRepository.load()
        if (!aggregate.hasValidCredentials()) {
            getUsername()
            getPassword()
        }

        authorized = tryAuth()
        while (!authorized && retry) {
            getPassword()
            authorized = tryAuth()
        }
    }

    override fun next() {
        if (authorized) {
            context.changeState(UpdateRepoState(context))
        } else {
            context.changeState(CloseState())
        }
    }

    private fun getUsername() {
        Logger.print("Enter username:")
        username = readLine() ?: ""
    }

    private fun getPassword() {
        Logger.print("Enter password:")
        password = PasswordHelper.readPassword()
    }

    private fun tryAuth(): Boolean {
        try {
            Logger.print("Signing in...")
            val passwordHash = PasswordHelper.hashPassword(password)
            val useCase = AuthenticateUserUseCase(context.apiPort, context.userRepository)
            val result = useCase.execute(
                username = username.ifEmpty {
                    context.userRepository.load().credentials?.username ?: ""
                },
                passwordHash = passwordHash.ifEmpty {
                    context.userRepository.load().credentials?.passwordHash ?: ""
                }
            )

            return when (result) {
                is AuthResult.Success -> {
                    Logger.print("Signed in successfully. Your profile page is " +
                        BuildConfig.PROFILE_URL + username)
                    Logger.username = username
                    Logger.info(Logger.Events.AUTH) { "Auth success" }
                    true
                }
                is AuthResult.Failure -> {
                    Logger.print("Authentication error. Try again.")
                    false
                }
            }
        } catch (e: Exception) {
            Logger.print("Connection problems. Try again later.")
            Logger.error(e)
            retry = false
            return false
        }
    }
}
