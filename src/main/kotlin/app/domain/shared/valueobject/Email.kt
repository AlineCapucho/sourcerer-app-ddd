package app.domain.shared.valueobject

/**
 * Objeto de valor que representa um endereço de email.
 * Imutável, com autovalidação, sem identidade própria.
 */
data class Email(private val address: String) {

    init {
        require(address.isNotBlank()) { "Email address cannot be blank" }
        require(address.contains("@")) { "Invalid email format: $address" }
    }

    override fun toString(): String = address.toLowerCase()

    fun value(): String = address.toLowerCase()
}
