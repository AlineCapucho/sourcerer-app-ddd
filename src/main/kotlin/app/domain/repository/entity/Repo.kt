package app.domain.repository.entity

import app.domain.repository.valueobject.RepoMeta

/**
 * Entidade que representa um repositório Git.
 * Identidade definida pelo rehash.
 * Expressividade: métodos com intenção de negócio.
 */
class Repo(
    val rehash: String,
    val initialCommitRehash: String = ""
) {
    private var _emails: List<String> = listOf()
    private var _commits: List<Commit> = listOf()
    private var _meta: RepoMeta = RepoMeta()
    private var _processEntryId: Int = 0

    val emails: List<String> get() = _emails
    val commits: List<Commit> get() = _commits
    val meta: RepoMeta get() = _meta
    val processEntryId: Int get() = _processEntryId

    init {
        require(rehash.isNotBlank()) { "Repo rehash cannot be blank" }
    }

    /**
     * Define os emails de filtro para hashing de commits.
     */
    fun defineEmailFilter(emails: List<String>) {
        _emails = emails
    }

    /**
     * Carrega o histórico de commits do servidor.
     */
    fun loadServerHistory(commits: List<Commit>) {
        _commits = commits
    }

    /**
     * Define os metadados do repositório.
     */
    fun defineMeta(meta: RepoMeta) {
        _meta = meta
    }

    /**
     * Define o ID da entrada de processamento.
     */
    fun defineProcessEntryId(id: Int) {
        _processEntryId = id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Repo) return false
        return rehash == other.rehash
    }

    override fun hashCode(): Int = rehash.hashCode()

    override fun toString(): String = "Repo(rehash=$rehash)"
}
