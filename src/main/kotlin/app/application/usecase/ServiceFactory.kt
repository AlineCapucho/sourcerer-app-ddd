package app.application.usecase

import app.domain.repository.service.AuthorDistanceService
import app.domain.repository.service.CodeLongevityService
import app.domain.repository.service.CommitExtractionService
import app.domain.repository.service.FactCalculationService
import app.domain.repository.service.MetaHashingService

/**
 * Factory para criação de services que dependem de dados dinâmicos (runtime).
 * Permite que o Use Case crie services com os parâmetros corretos
 * após descobrir emails e total de commits do repositório.
 */
interface ServiceFactory {
    fun createFactCalculationService(
        emails: Set<String>,
        totalCommits: Int
    ): FactCalculationService

    fun createAuthorDistanceService(
        allEmails: Set<String>,
        userEmails: Set<String>
    ): AuthorDistanceService

    fun createCommitExtractionService(): CommitExtractionService

    fun createMetaHashingService(): MetaHashingService

    fun createCodeLongevityService(): CodeLongevityService
}
