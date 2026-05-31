package app.domain.repository.service

import app.domain.repository.entity.Author
import app.domain.repository.valueobject.Fact

/**
 * Domain Service: Cálculo de metadados do repositório.
 * Operação sem estado (stateless) que realiza uma tarefa do domínio.
 *
 * Responsabilidade: Calcular fatos de metadados como tamanho do time,
 * share de commits, etc.
 */
interface MetaHashingService {
    /**
     * Calcula fatos de metadados para o repositório.
     */
    fun calculateMetaFacts(
        repoRehash: String,
        authors: Set<Author>,
        commitsCount: Map<String, Int>,
        userEmails: List<String>
    ): List<Fact>
}
