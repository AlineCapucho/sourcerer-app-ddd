package app.infrastructure.extractor

/**
 * Detecção de linguagem por extensão de arquivo.
 * Equivalente simplificado do Heuristics.kt original.
 */
object LanguageDetector {

    private val extensionToLanguage = mapOf(
        "kt" to "kotlin",
        "java" to "java",
        "py" to "python",
        "js" to "javascript",
        "ts" to "typescript",
        "tsx" to "typescript",
        "jsx" to "javascript",
        "rb" to "ruby",
        "go" to "go",
        "rs" to "rust",
        "c" to "c",
        "h" to "c",
        "cpp" to "cpp",
        "cc" to "cpp",
        "cxx" to "cpp",
        "hpp" to "cpp",
        "cs" to "csharp",
        "swift" to "swift",
        "m" to "objectivec",
        "scala" to "scala",
        "clj" to "clojure",
        "hs" to "haskell",
        "erl" to "erlang",
        "ex" to "elixir",
        "exs" to "elixir",
        "php" to "php",
        "pl" to "perl",
        "pm" to "perl",
        "r" to "r",
        "R" to "r",
        "lua" to "lua",
        "dart" to "dart",
        "groovy" to "groovy",
        "sh" to "shell",
        "bash" to "shell",
        "zsh" to "shell",
        "css" to "css",
        "scss" to "scss",
        "less" to "less",
        "html" to "html",
        "htm" to "html",
        "xml" to "xml",
        "json" to "json",
        "yaml" to "yaml",
        "yml" to "yaml",
        "sql" to "sql",
        "md" to "markdown",
        "coffee" to "coffeescript",
        "fs" to "fsharp",
        "fsx" to "fsharp",
        "vb" to "visualbasic",
        "d" to "d",
        "cr" to "crystal",
        "nim" to "nim",
        "vue" to "vue",
        "svelte" to "svelte"
    )

    fun detectByExtension(extension: String): String? {
        val ext = extension.removePrefix(".")
        return extensionToLanguage[ext]
    }
}
