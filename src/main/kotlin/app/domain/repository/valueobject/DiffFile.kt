package app.domain.repository.valueobject

/**
 * Objeto de valor que representa um arquivo modificado em um commit.
 * Imutável, sem identidade própria.
 */
data class DiffFile(
    val path: String,
    val changeType: ChangeType,
    val old: DiffContent = DiffContent(),
    val new: DiffContent = DiffContent(),
    val lang: String = ""
) {
    val extension: String = extractExtension(path)

    fun getAllAdded(): List<String> = new.getAllDiffs()

    fun getAllDeleted(): List<String> = old.getAllDiffs()

    /**
     * Cria uma cópia com o idioma definido.
     * Objetos de valor são imutáveis — retorna nova instância.
     */
    fun withLanguage(language: String): DiffFile = copy(lang = language)

    /**
     * Cria uma cópia com imports definidos no conteúdo new.
     */
    fun withImports(imports: List<String>): DiffFile = copy(
        new = DiffContent(new.content, new.ranges, imports)
    )

    private fun extractExtension(filePath: String): String {
        val dotIndex = filePath.lastIndexOf('.')
        return if (dotIndex >= 0) filePath.substring(dotIndex) else ""
    }
}

enum class ChangeType {
    ADD,
    MODIFY,
    DELETE,
    RENAME,
    COPY
}
