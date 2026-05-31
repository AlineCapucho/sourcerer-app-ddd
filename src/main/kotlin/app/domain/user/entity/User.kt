package app.domain.user.entity

import app.domain.user.valueobject.UserEmail

/**
 * Entidade que representa um usuário do sistema.
 * Identidade definida pelo uuid.
 * Expressividade: métodos com intenção de negócio.
 */
class User(
    val uuid: String = ""
) {
    private val _emails: MutableSet<UserEmail> = mutableSetOf()
    private val _localRepoPaths: MutableSet<String> = mutableSetOf()

    val emails: Set<UserEmail> get() = _emails.toSet()
    val localRepoPaths: Set<String> get() = _localRepoPaths.toSet()

    /**
     * Registra um email associado ao usuário.
     */
    fun registerEmail(email: UserEmail) {
        _emails.remove(email) // Remove para atualizar campos
        _emails.add(email)
    }

    /**
     * Registra múltiplos emails.
     */
    fun registerEmails(emails: Collection<UserEmail>) {
        emails.forEach { registerEmail(it) }
    }

    /**
     * Retorna os endereços de email como strings.
     */
    fun emailAddresses(): List<String> = _emails.map { it.address }

    /**
     * Adiciona um repositório local ao rastreamento.
     */
    fun trackRepository(path: String) {
        require(path.isNotBlank()) { "Repository path cannot be blank" }
        _localRepoPaths.add(path)
    }

    /**
     * Remove um repositório local do rastreamento.
     */
    fun untrackRepository(path: String) {
        _localRepoPaths.remove(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return uuid == other.uuid
    }

    override fun hashCode(): Int = uuid.hashCode()
}
