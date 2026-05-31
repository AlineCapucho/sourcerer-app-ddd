package app.infrastructure.extractor

import app.domain.repository.service.AuthorDistanceService
import app.domain.repository.valueobject.AuthorDistance
import app.domain.shared.valueobject.Email

/**
 * Implementação do AuthorDistanceService.
 * Calcula métricas de proximidade entre autores baseado nos caminhos
 * de arquivos que modificaram.
 */
class DefaultAuthorDistanceService(
    private val allEmails: Set<String>,
    private val userEmails: Set<String>
) : AuthorDistanceService {

    // Map de email -> set de caminhos modificados
    private val authorPaths = hashMapOf<String, MutableSet<String>>()

    override fun registerContribution(email: Email, paths: List<String>, timestamp: Long) {
        val emailStr = email.value()
        if (!allEmails.contains(emailStr)) return

        val pathSet = authorPaths.getOrPut(emailStr) { mutableSetOf() }
        pathSet.addAll(paths)
    }

    override fun calculateDistances(repoRehash: String): List<AuthorDistance> {
        val distances = mutableListOf<AuthorDistance>()

        for (userEmail in userEmails) {
            val userPaths = authorPaths[userEmail] ?: continue

            for ((otherEmail, otherPaths) in authorPaths) {
                if (otherEmail == userEmail) continue

                // Jaccard similarity between path sets
                val intersection = userPaths.intersect(otherPaths).size
                val union = userPaths.union(otherPaths).size

                if (union > 0) {
                    val score = intersection.toDouble() / union.toDouble()
                    if (score > 0) {
                        distances.add(AuthorDistance(
                            repoRehash = repoRehash,
                            email = Email(otherEmail),
                            score = score
                        ))
                    }
                }
            }
        }

        return distances
    }
}
