package app.application.mapper

import app.application.dto.CommitDTO
import app.application.dto.CommitStatsDTO
import app.domain.repository.entity.Commit

/**
 * Mapper: Mapeamento das informações entre as camadas.
 * Converte entre entidades de domínio e DTOs de aplicação.
 */
object CommitMapper {

    fun toDTO(commit: Commit): CommitDTO {
        return CommitDTO(
            rehash = commit.rehash,
            repoRehash = commit.repoRehash,
            treeRehash = commit.treeRehash,
            authorName = commit.author.name,
            authorEmail = commit.author.email.value(),
            dateTimestamp = commit.dateTimestamp,
            isQommit = commit.isQommit,
            numLinesAdded = commit.numLinesAdded,
            numLinesDeleted = commit.numLinesDeleted,
            stats = commit.stats.map { stat ->
                CommitStatsDTO(
                    numLinesAdded = stat.numLinesAdded,
                    numLinesDeleted = stat.numLinesDeleted,
                    type = stat.type,
                    tech = stat.tech
                )
            }
        )
    }
}
