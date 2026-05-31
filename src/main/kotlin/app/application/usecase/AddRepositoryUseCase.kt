package app.application.usecase

import app.domain.user.repository.UserRepository
import app.domain.user.valueobject.LocalRepo

/**
 * Use Case: Adicionar um repositório ao rastreamento.
 * Camada intermediária que coordena as interações entre domínio e infraestrutura.
 */
class AddRepositoryUseCase(
    private val userRepository: UserRepository,
    private val gitValidator: GitRepositoryValidator
) {
    fun execute(path: String, hashAllContributors: Boolean = false): Boolean {
        if (!gitValidator.isValidRepo(path)) {
            return false
        }

        val aggregate = userRepository.load()
        val localRepo = LocalRepo(
            path = path,
            hashAllContributors = hashAllContributors
        )
        aggregate.addLocalRepo(localRepo)
        userRepository.save(aggregate)
        return true
    }
}

/**
 * Interface para validação de repositórios Git.
 * Implementada na infraestrutura.
 */
interface GitRepositoryValidator {
    fun isValidRepo(path: String): Boolean
    fun isValidGitRepo(path: String): Boolean
}
