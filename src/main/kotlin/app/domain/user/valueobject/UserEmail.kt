package app.domain.user.valueobject

/**
 * Objeto de valor que representa um email de usuário com status.
 * Imutável, sem identidade própria, com autovalidação.
 */
data class UserEmail(
    val address: String,
    val primary: Boolean = false,
    val verified: Boolean = false
) {
    init {
        require(address.isNotBlank()) { "Email address cannot be blank" }
        require(address.contains("@")) { "Invalid email format: $address" }
    }

    override fun toString(): String {
        val primaryLabel = if (primary) " (Primary)" else ""
        val verifiedLabel = if (verified) "Confirmed" else "Not confirmed"
        return "$address$primaryLabel — $verifiedLabel"
    }

    override fun equals(other: Any?): Boolean {
        if (other is UserEmail) {
            return address == other.address
        }
        return false
    }

    override fun hashCode(): Int = address.hashCode()
}
