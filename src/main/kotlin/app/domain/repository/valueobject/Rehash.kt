package app.domain.repository.valueobject

/**
 * Objeto de valor que representa um rehash (SHA-256 hex string).
 * Imutável, com autovalidação, sem identidade própria.
 */
data class Rehash(private val hash: String) {

    init {
        require(hash.isNotBlank()) { "Rehash cannot be blank" }
        require(hash.length == 64) { "Rehash must be a 64-character SHA-256 hex string" }
        require(hash.all { it in '0'..'9' || it in 'a'..'f' }) {
            "Rehash must contain only hexadecimal characters"
        }
    }

    fun value(): String = hash

    override fun toString(): String = hash
}
