package app.domain.repository.factory

import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.valueobject.DiffFile
import app.domain.shared.valueobject.Email

/**
 * Factory para criação de instâncias complexas de Commit.
 * Responsabilidade de criar instâncias complexas e agregados em um objeto separado.
 * Interface que encapsula toda a criação.
 * Não faz referência a objetos concretos que estão sendo instanciados (JGit).
 */
object CommitFactory {

    /**
     * Cria um Commit a partir de dados brutos extraídos do Git.
     * Encapsula a complexidade de construção.
     */
    fun create(
        rehash: String,
        authorName: String,
        authorEmail: String,
        dateTimestamp: Long,
        dateTimeZoneOffset: Int,
        treeRehash: String,
        coauthors: List<Pair<String, String>> = listOf(),
        diffs: List<DiffFile> = listOf(),
        repoRehash: String = ""
    ): Commit {
        val author = Author(
            email = Email(authorEmail),
            name = authorName
        )

        val coauthorEntities = coauthors.map { (name, email) ->
            Author(email = Email(email), name = name)
        }

        val commit = Commit(
            rehash = rehash,
            author = author,
            dateTimestamp = dateTimestamp,
            dateTimeZoneOffset = dateTimeZoneOffset,
            treeRehash = treeRehash,
            coauthors = coauthorEntities
        )

        if (diffs.isNotEmpty()) {
            commit.registerDiffs(diffs)
        }

        if (repoRehash.isNotBlank()) {
            commit.associateToRepo(repoRehash)
        }

        return commit
    }

    /**
     * Reconstrói um Commit a partir de dados do servidor (sem diffs).
     */
    fun reconstruct(
        rehash: String,
        authorName: String,
        authorEmail: String,
        dateTimestamp: Long,
        repoRehash: String,
        treeRehash: String = "",
        isQommit: Boolean = false,
        numLinesAdded: Int = 0,
        numLinesDeleted: Int = 0
    ): Commit {
        val commit = create(
            rehash = rehash,
            authorName = authorName,
            authorEmail = authorEmail,
            dateTimestamp = dateTimestamp,
            dateTimeZoneOffset = 0,
            treeRehash = treeRehash,
            repoRehash = repoRehash
        )

        if (isQommit) {
            commit.markAsQommit()
        }

        return commit
    }
}
