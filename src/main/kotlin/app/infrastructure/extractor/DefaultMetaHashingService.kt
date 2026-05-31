package app.infrastructure.extractor

import app.domain.repository.FactCodes
import app.domain.repository.entity.Author
import app.domain.repository.service.MetaHashingService
import app.domain.repository.valueobject.Fact
import app.domain.shared.valueobject.Email

/**
 * Implementação do MetaHashingService.
 * Calcula fatos de metadados como tamanho do time e share de commits.
 */
class DefaultMetaHashingService : MetaHashingService {

    override fun calculateMetaFacts(
        repoRehash: String,
        authors: Set<Author>,
        commitsCount: Map<String, Int>,
        userEmails: List<String>
    ): List<Fact> {
        val facts = mutableListOf<Fact>()

        // Team size fact
        val teamSize = authors.size
        userEmails.forEach { email ->
            facts.add(Fact(
                repoRehash = repoRehash,
                code = FactCodes.REPO_TEAM_SIZE,
                key = 0,
                value = teamSize.toString(),
                authorEmail = Email(email)
            ))
        }

        // Commit share facts
        val totalCommits = commitsCount.values.sum()
        if (totalCommits > 0) {
            userEmails.forEach { userEmail ->
                val userCommits = commitsCount.getOrDefault(userEmail, 0)
                if (userCommits > 0) {
                    val share = userCommits.toDouble() / totalCommits
                    facts.add(Fact(
                        repoRehash = repoRehash,
                        code = FactCodes.COMMIT_SHARE,
                        key = 0,
                        value = share.toString(),
                        authorEmail = Email(userEmail)
                    ))
                }

                // Colleagues fact
                val colleagues = commitsCount.keys
                    .filter { it != userEmail }
                    .size
                if (colleagues > 0) {
                    facts.add(Fact(
                        repoRehash = repoRehash,
                        code = FactCodes.COLLEAGUES,
                        key = 0,
                        value = colleagues.toString(),
                        authorEmail = Email(userEmail)
                    ))
                }
            }
        }

        return facts
    }
}
