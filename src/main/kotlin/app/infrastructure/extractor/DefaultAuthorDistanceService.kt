package app.infrastructure.extractor

import app.domain.repository.service.AuthorDistanceService
import app.domain.repository.valueobject.AuthorDistance
import app.domain.shared.valueobject.Email
import java.util.concurrent.TimeUnit

/**
 * Implementação do AuthorDistanceService.
 * Calcula métricas de proximidade entre autores baseado em sobreposição
 * temporal de caminhos de arquivos modificados.
 *
 * Algoritmo: Para cada commit do usuário, armazena o timestamp da última
 * modificação em cada path. Para commits de outros autores, conta os paths
 * que se sobrepõem com os do usuário E estão dentro de 365 dias.
 */
class DefaultAuthorDistanceService(
    private val allEmails: Set<String>,
    private val userEmails: Set<String>
) : AuthorDistanceService {

    // Map de path -> timestamp da última contribuição do usuário
    private val authorPathLastContribution = hashMapOf<String, Long>()

    // Map de email -> score acumulado
    private val authorScores = hashMapOf<String, Double>()

    init {
        allEmails.forEach { authorScores[it] = 0.0 }
    }

    override fun registerContribution(email: Email, paths: List<String>, timestamp: Long) {
        val emailStr = email.value()
        if (!allEmails.contains(emailStr)) return

        if (emailStr in userEmails) {
            // Para commits do usuário: armazena o timestamp de cada path
            paths.forEach { path -> authorPathLastContribution[path] = timestamp }
        } else {
            // Para outros autores: conta paths que sobrepõem com user E dentro de 365 dias
            val score = paths
                .filter { path -> path in authorPathLastContribution }
                .filter { path ->
                    val authorTime = authorPathLastContribution[path]!!
                    val timeDelta = TimeUnit.DAYS.convert(
                        authorTime - timestamp, TimeUnit.SECONDS
                    )
                    timeDelta < 365
                }.size
            authorScores[emailStr] = (authorScores[emailStr] ?: 0.0) + score
        }
    }

    override fun calculateDistances(repoRehash: String): List<AuthorDistance> {
        val distances = mutableListOf<AuthorDistance>()

        for ((email, score) in authorScores) {
            if (email in userEmails) continue
            distances.add(AuthorDistance(
                repoRehash = repoRehash,
                email = Email(email),
                score = score
            ))
        }

        return distances
    }
}
