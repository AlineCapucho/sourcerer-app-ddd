package app.application.mapper

import app.application.dto.RepoDTO
import app.application.dto.RepoMetaDTO
import app.domain.repository.entity.Repo

/**
 * Mapper: Mapeamento das informações entre as camadas.
 * Converte entre entidades de domínio e DTOs de aplicação.
 */
object RepoMapper {

    fun toDTO(repo: Repo): RepoDTO {
        return RepoDTO(
            rehash = repo.rehash,
            initialCommitRehash = repo.initialCommitRehash,
            emails = repo.emails,
            meta = RepoMetaDTO(
                hosterId = repo.meta.hosterId,
                service = repo.meta.service,
                name = repo.meta.name,
                ownerName = repo.meta.ownerName,
                description = repo.meta.description,
                htmlUrl = repo.meta.htmlUrl,
                cloneUrl = repo.meta.cloneUrl
            ),
            processEntryId = repo.processEntryId
        )
    }
}
