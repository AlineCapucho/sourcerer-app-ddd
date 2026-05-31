package app.domain.repository.entity

import app.domain.repository.valueobject.CommitStats
import app.domain.repository.valueobject.DiffFile
import app.domain.shared.valueobject.Email

/**
 * Entidade que representa um commit no repositório.
 * Identidade definida pelo rehash (SHA-256 do hash original).
 * Expressividade: métodos com intenção de negócio.
 */
class Commit(
    val rehash: String,
    val author: Author,
    val dateTimestamp: Long,
    val dateTimeZoneOffset: Int = 0,
    val treeRehash: String = "",
    val coauthors: List<Author> = listOf()
) {
    private var _repoRehash: String = ""
    private var _stats: List<CommitStats> = listOf()
    private var _diffs: List<DiffFile> = listOf()
    private var _numLinesAdded: Int = 0
    private var _numLinesDeleted: Int = 0
    private var _isQommit: Boolean = false

    val repoRehash: String get() = _repoRehash
    val stats: List<CommitStats> get() = _stats
    val diffs: List<DiffFile> get() = _diffs
    val numLinesAdded: Int get() = _numLinesAdded
    val numLinesDeleted: Int get() = _numLinesDeleted
    val isQommit: Boolean get() = _isQommit

    init {
        require(rehash.isNotBlank()) { "Commit rehash cannot be blank" }
    }

    /**
     * Associa este commit a um repositório.
     */
    fun associateToRepo(repoRehash: String) {
        _repoRehash = repoRehash
    }

    /**
     * Registra os diffs extraídos do commit e calcula linhas totais.
     */
    fun registerDiffs(diffs: List<DiffFile>) {
        _diffs = diffs
        _numLinesAdded = diffs.fold(0) { total, file ->
            total + file.getAllAdded().size
        }
        _numLinesDeleted = diffs.fold(0) { total, file ->
            total + file.getAllDeleted().size
        }
    }

    /**
     * Registra as estatísticas de tecnologia extraídas do commit.
     */
    fun registerStats(stats: List<CommitStats>) {
        _stats = stats
    }

    /**
     * Marca como quality commit.
     */
    fun markAsQommit() {
        _isQommit = true
    }

    /**
     * Retorna todas as linhas adicionadas em todos os diffs.
     */
    fun getAllAdded(): List<String> = _diffs.flatMap { it.getAllAdded() }

    /**
     * Retorna todas as linhas deletadas em todos os diffs.
     */
    fun getAllDeleted(): List<String> = _diffs.flatMap { it.getAllDeleted() }

    /**
     * Cria uma cópia do commit com um autor diferente (para coautores).
     */
    fun copyForCoauthor(coauthor: Author): Commit {
        val copy = Commit(
            rehash = rehash,
            author = coauthor,
            dateTimestamp = dateTimestamp,
            dateTimeZoneOffset = dateTimeZoneOffset,
            treeRehash = treeRehash
        )
        copy._repoRehash = _repoRehash
        copy._stats = _stats
        copy._diffs = _diffs
        copy._numLinesAdded = _numLinesAdded
        copy._numLinesDeleted = _numLinesDeleted
        copy._isQommit = _isQommit
        return copy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Commit) return false
        return rehash == other.rehash
    }

    override fun hashCode(): Int = rehash.hashCode()

    override fun toString(): String = "Commit(rehash=$rehash, author=$author)"
}
