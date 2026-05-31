package app.domain.repository.entity

import app.domain.shared.valueobject.Email

/**
 * Entidade que representa um autor de commits.
 * Identidade definida pelo email.
 * Expressividade: métodos com intenção de negócio.
 */
class Author(
    val email: Email,
    name: String = ""
) {
    private var _name: String = name

    var repoRehash: String = ""
        private set

    val name: String get() = _name

    /**
     * Associa este autor a um repositório.
     */
    fun associateToRepo(rehash: String) {
        repoRehash = rehash
    }

    /**
     * Atualiza o nome do autor se o novo nome for mais completo.
     * Intenção de negócio: preferir nomes mais longos (mais completos).
     */
    fun updateNameIfMoreComplete(newName: String) {
        if (newName.length > _name.length) {
            _name = newName
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Author) return false
        return email == other.email
    }

    override fun hashCode(): Int = email.hashCode()

    override fun toString(): String = "$_name <${email.value()}>"
}
