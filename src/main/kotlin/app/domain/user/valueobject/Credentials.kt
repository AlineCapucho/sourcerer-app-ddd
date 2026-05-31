package app.domain.user.valueobject

/**
 * Objeto de valor que representa credenciais de autenticação.
 * Imutável, sem identidade própria, com autovalidação.
 */
data class Credentials(
    val username: String,
    val passwordHash: String
) {
    init {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(passwordHash.isNotBlank()) { "Password hash cannot be blank" }
    }

    fun isValid(): Boolean = username.isNotBlank() && passwordHash.isNotBlank()
}
