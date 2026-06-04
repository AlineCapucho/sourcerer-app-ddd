package app.infrastructure.extractor

import app.domain.repository.FactCodes
import app.domain.repository.entity.Author
import app.domain.repository.service.MetaHashingService
import app.domain.repository.valueobject.Fact
import app.domain.shared.valueobject.Email

/**
 * Implementação do MetaHashingService.
 * Calcula fatos de metadados como tamanho do time e share de commits.
 * Usa deduplicação de autores baseada em similaridade de Jaccard com 3-grams.
 */
class DefaultMetaHashingService : MetaHashingService {

    override fun calculateMetaFacts(
        repoRehash: String,
        authors: Set<Author>,
        commitsCount: Map<String, Int>,
        userEmails: List<String>
    ): List<Fact> {
        val facts = mutableListOf<Fact>()

        val otherAuthors = authors.filter { author ->
            !userEmails.contains(author.email.value())
        }
        val isUserAuthor = otherAuthors.size < authors.size
        val numAuthors = getAuthorsNum(otherAuthors) + if (isUserAuthor) 1 else 0

        // REPO_TEAM_SIZE (repo-level fact, no specific author)
        facts.add(Fact(
            repoRehash = repoRehash,
            code = FactCodes.REPO_TEAM_SIZE,
            key = 0,
            value = numAuthors.toString(),
            authorEmail = Email("")
        ))

        // COMMIT_SHARE_REPO_AVG (repo-level fact, no specific author)
        val numAllCommits = commitsCount.values.fold(0) { acc, i -> acc + i }
        val avgCommits = Math.round(numAllCommits.toDouble() / numAuthors).toInt()
        facts.add(Fact(
            repoRehash = repoRehash,
            code = FactCodes.COMMIT_SHARE_REPO_AVG,
            key = 0,
            value = avgCommits.toString(),
            authorEmail = Email("")
        ))

        // COMMIT_SHARE (absolute count for user)
        if (isUserAuthor) {
            val numUserCommits = userEmails
                .mapNotNull { email -> commitsCount[email] }
                .fold(0) { acc, i -> acc + i }
            val userEmail = userEmails.first()
            facts.add(Fact(
                repoRehash = repoRehash,
                code = FactCodes.COMMIT_SHARE,
                key = 0,
                value = numUserCommits.toString(),
                authorEmail = Email(userEmail)
            ))
        }

        return facts
    }

    /**
     * Deduplica autores usando similaridade de Jaccard com 3-grams.
     * Autores com Jaccard >= 0.3 nos nomes OU prefixos de email são
     * considerados a mesma pessoa.
     */
    private fun getAuthorsNum(authors: List<Author>): Int {
        val names = authors.map { it.name }
        val emails = authors.map { it.email.value().split("@")[0] }
        val namesQgrams = names.map { getThreegrams(it) }
        val emailsQgrams = emails.map { getThreegrams(it) }
        val results = Array(authors.size) { Array(authors.size) { 0 } }
        for (i in 0..authors.size - 2) {
            for (j in i + 1 until authors.size) {
                if (isSameAuthor(namesQgrams[i], namesQgrams[j])) results[j][i] = 1
                if (isSameAuthor(emailsQgrams[i], emailsQgrams[j])) results[j][i] = 1
            }
        }
        return results.filter { it.sum() == 0 }.size
    }

    private fun isSameAuthor(first: Set<String>, second: Set<String>): Boolean {
        val intersectionSize = first.intersect(second).size
        val unionSize = first.union(second).size
        if (unionSize == 0) return false
        val jaccardValue = intersectionSize.toFloat() / unionSize
        return jaccardValue >= 0.3
    }

    private fun getThreegrams(str: String): Set<String> {
        val threegrams = mutableSetOf<String>()
        for (i in 0..str.length - 3) {
            threegrams.add(listOf(str[i], str[i + 1], str[i + 2]).joinToString(""))
        }
        return threegrams
    }
}
