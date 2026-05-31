package app.infrastructure.extractor

import app.domain.repository.service.CommitExtractionService
import app.domain.repository.valueobject.CommitStats
import app.domain.repository.valueobject.DiffContent
import app.domain.repository.valueobject.DiffFile

/**
 * Implementação do CommitExtractionService.
 * Extrai estatísticas de linguagem e biblioteca dos diffs.
 */
class DefaultCommitExtractionService : CommitExtractionService {

    companion object {
        val RESTRICTED_EXTS = listOf(".min.js")
        val SEPARATOR = ">"
        val stringRegex = Regex("""(".+?"|'.+?')""")
    }

    override fun extractStats(files: List<DiffFile>): List<CommitStats> {
        return files
            .filter { file -> !RESTRICTED_EXTS.contains(file.extension) }
            .mapNotNull { file -> analyzeFile(file) }
            .fold(mutableListOf()) { accStats, stats ->
                accStats.addAll(stats)
                accStats
            }
    }

    override fun tokenize(line: String): List<String> {
        val newLine = stringRegex.replace(line, "")
        return newLine.split(' ', '[', ',', ';', '*', '\n', ')', '(',
            '[', ']', '}', '{', '+', '-', '=', '&', '$', '!', '.', '>',
            '<', '#', '@', ':', '?', ']')
            .filter {
                it.isNotBlank() && !it.contains('"') && !it.contains('\'') &&
                    it != "-" && it != "@"
            }
    }

    private fun analyzeFile(file: DiffFile): List<CommitStats>? {
        val language = LanguageDetector.detectByExtension(file.extension)
            ?: return null

        val fileWithLang = file.withLanguage(language)

        val langStats = listOf(CommitStats(
            numLinesAdded = fileWithLang.getAllAdded().size,
            numLinesDeleted = fileWithLang.getAllDeleted().size,
            type = CommitStats.TYPE_LANGUAGE,
            tech = language
        )).filter { it.numLinesAdded > 0 || it.numLinesDeleted > 0 }

        return langStats
    }
}
