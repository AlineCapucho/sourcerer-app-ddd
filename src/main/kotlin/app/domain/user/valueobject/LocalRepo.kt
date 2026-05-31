package app.domain.user.valueobject

import app.domain.repository.valueobject.RepoMeta

/**
 * Objeto de valor que representa um repositório local rastreado.
 * Imutável, sem identidade própria.
 */
data class LocalRepo(
    val path: String,
    val hashAllContributors: Boolean = false,
    val remoteOrigin: String = "",
    val meta: RepoMeta = RepoMeta(),
    val processEntryId: Int? = null
) {
    init {
        require(path.isNotBlank()) { "Local repo path cannot be blank" }
    }

    override fun toString(): String = path

    override fun equals(other: Any?): Boolean {
        if (other is LocalRepo) {
            return path == other.path
        }
        return false
    }

    override fun hashCode(): Int = path.hashCode()
}
