package app.application.usecase

import app.domain.user.repository.UserRepository

/**
 * Use Case: Remover um repositório do rastreamento.
 * Camada intermediária que coordena as interações entre domínio e infraestrutura.
 */
class RemoveRepositoryUseCase(
    private val userRepository: UserRepository
) {
    fun execute(path: String) {
        val aggregate = userRepository.load()
        aggregate.removeLocalRepo(path)
        userRepository.save(aggregate)
    }
}
