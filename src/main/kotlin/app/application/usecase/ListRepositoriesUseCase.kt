package app.application.usecase

import app.domain.user.repository.UserRepository
import app.domain.user.valueobject.LocalRepo

/**
 * Use Case: Listar repositórios rastreados.
 * Camada intermediária que coordena as interações entre domínio e infraestrutura.
 */
class ListRepositoriesUseCase(
    private val userRepository: UserRepository
) {
    fun execute(): List<LocalRepo> {
        val aggregate = userRepository.load()
        return aggregate.getLocalRepos()
    }
}
