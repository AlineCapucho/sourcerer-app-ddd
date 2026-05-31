package app.application.usecase

import app.domain.repository.port.ServerApiPort
import app.domain.user.entity.User
import app.domain.user.repository.UserRepository
import app.domain.user.valueobject.Credentials
import app.domain.user.valueobject.UserEmail

/**
 * Use Case: Autenticar o usuário no servidor.
 * Camada intermediária que coordena as interações entre domínio e infraestrutura.
 */
class AuthenticateUserUseCase(
    private val apiPort: ServerApiPort,
    private val userRepository: UserRepository
) {
    fun execute(username: String, passwordHash: String): AuthResult {
        // Autenticar no servidor
        val authResult = apiPort.authorize(username, passwordHash)
        if (authResult is app.domain.repository.port.ApiResult.Error) {
            return AuthResult.Failure(authResult.code, authResult.message)
        }

        // Buscar dados do usuário
        val userDataResult = apiPort.getUser()
        if (userDataResult is app.domain.repository.port.ApiResult.Error) {
            return AuthResult.Failure(userDataResult.code, userDataResult.message)
        }

        val userData = (userDataResult as app.domain.repository.port.ApiResult.Success).data

        // Atualizar agregado do usuário
        val aggregate = userRepository.load()
        aggregate.defineCredentials(Credentials(username, passwordHash))

        val userEmails = userData.emails
        aggregate.user.registerEmails(userEmails)

        userRepository.save(aggregate)

        return AuthResult.Success(userEmails)
    }
}

sealed class AuthResult {
    data class Success(val emails: List<UserEmail>) : AuthResult()
    data class Failure(val code: Int, val message: String) : AuthResult()
}
