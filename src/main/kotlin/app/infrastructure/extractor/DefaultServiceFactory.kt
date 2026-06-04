package app.infrastructure.extractor

import app.application.usecase.ServiceFactory
import app.domain.repository.service.AuthorDistanceService
import app.domain.repository.service.CodeLongevityService
import app.domain.repository.service.CommitExtractionService
import app.domain.repository.service.FactCalculationService
import app.domain.repository.service.MetaHashingService

/**
 * Implementação da factory de services na infraestrutura.
 * Cria instâncias com os parâmetros dinâmicos corretos.
 */
class DefaultServiceFactory : ServiceFactory {

    override fun createFactCalculationService(
        emails: Set<String>,
        totalCommits: Int
    ): FactCalculationService {
        return DefaultFactCalculationService(
            emails = emails,
            totalCommits = totalCommits,
            extractionService = createCommitExtractionService()
        )
    }

    override fun createAuthorDistanceService(
        allEmails: Set<String>,
        userEmails: Set<String>
    ): AuthorDistanceService {
        return DefaultAuthorDistanceService(
            allEmails = allEmails,
            userEmails = userEmails
        )
    }

    override fun createCommitExtractionService(): CommitExtractionService {
        return DefaultCommitExtractionService()
    }

    override fun createMetaHashingService(): MetaHashingService {
        return DefaultMetaHashingService()
    }

    override fun createCodeLongevityService(): CodeLongevityService {
        return DefaultCodeLongevityService()
    }
}
