package app.infrastructure.extractor

import app.domain.repository.FactCodes
import app.domain.repository.entity.Commit
import app.domain.repository.service.CommitExtractionService
import app.domain.repository.service.FactCalculationService
import app.domain.repository.valueobject.Fact
import app.domain.shared.valueobject.Email
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Implementação do FactCalculationService.
 * Calcula fatos/estatísticas sobre padrões de desenvolvimento.
 */
class DefaultFactCalculationService(
    private val emails: Set<String>,
    private val totalCommits: Int,
    private val extractionService: CommitExtractionService
) : FactCalculationService {

    private val fsDayWeek = hashMapOf<String, Array<Int>>()
    private val fsDayTime = hashMapOf<String, Array<Int>>()
    private val fsRepoDateStart = hashMapOf<String, Long>()
    private val fsRepoDateEnd = hashMapOf<String, Long>()
    private val fsCommitLineNumAvg = hashMapOf<String, Double>()
    private val fsCommitNum = hashMapOf<String, Int>()
    private val fsLineLenAvg = hashMapOf<String, Double>()
    private val fsLineNum = hashMapOf<String, Long>()
    private val fsLinesPerCommits = hashMapOf<String, Array<Int>>()
    private val fsVariableNaming = hashMapOf<String, Array<Int>>()
    private val fsIndentation = hashMapOf<String, Array<Int>>()

    private val varNamingRegex = Regex("[a-z][A-Z]")

    init {
        for (author in emails) {
            fsDayWeek[author] = Array(7) { 0 }
            fsDayTime[author] = Array(24) { 0 }
            fsRepoDateStart[author] = -1
            fsRepoDateEnd[author] = -1
            fsCommitLineNumAvg[author] = 0.0
            fsCommitNum[author] = 0
            fsLineLenAvg[author] = 0.0
            fsLineNum[author] = 0
            fsLinesPerCommits[author] = Array(totalCommits) { 0 }
            fsVariableNaming[author] = Array(3) { 0 }
            fsIndentation[author] = Array(2) { 0 }
        }
    }

    override fun processCommit(commit: Commit) {
        val email = commit.author.email.value()
        if (!emails.contains(email)) return

        val timestamp = commit.dateTimestamp
        val dateTime = LocalDateTime.ofEpochSecond(timestamp, 0,
            ZoneOffset.ofTotalSeconds(commit.dateTimeZoneOffset * 60))

        // DayWeek
        val factDayWeek = fsDayWeek[email] ?: Array(7) { 0 }
        factDayWeek[dateTime.dayOfWeek.value - 1] += 1
        fsDayWeek[email] = factDayWeek

        // DayTime
        val factDayTime = fsDayTime[email] ?: Array(24) { 0 }
        factDayTime[dateTime.hour] += 1
        fsDayTime[email] = factDayTime

        // RepoDateStart
        fsRepoDateStart[email] = timestamp

        // RepoDateEnd
        if (fsRepoDateEnd[email]!! == -1L) {
            fsRepoDateEnd[email] = timestamp
        }

        // Commits
        val numCommits = fsCommitNum[email]!! + 1
        val numLinesCurrent = commit.numLinesAdded + commit.numLinesDeleted

        fsCommitNum[email] = numCommits
        fsCommitLineNumAvg[email] = calcIncAvg(fsCommitLineNumAvg[email]!!,
            numLinesCurrent.toDouble(), numCommits.toLong())

        val lines = commit.getAllAdded() + commit.getAllDeleted()
        lines.forEachIndexed { index, line ->
            fsLineLenAvg[email] = calcIncAvg(fsLineLenAvg[email]!!,
                line.length.toDouble(), fsLineNum[email]!! + index + 1)
        }
        fsLineNum[email] = fsLineNum[email]!! + lines.size

        if (numCommits - 1 < fsLinesPerCommits[email]!!.size) {
            fsLinesPerCommits[email]!![numCommits - 1] += lines.size
        }

        // Variable naming
        lines.forEach { line ->
            val tokens = extractionService.tokenize(line)
            val underscores = tokens.count { it.contains('_') }
            val camelCases = tokens.count {
                !it.contains('_') && it.contains(varNamingRegex)
            }
            val others = tokens.size - underscores - camelCases
            fsVariableNaming[email]!![FactCodes.VARIABLE_NAMING_SNAKE_CASE] += underscores
            fsVariableNaming[email]!![FactCodes.VARIABLE_NAMING_CAMEL_CASE] += camelCases
            fsVariableNaming[email]!![FactCodes.VARIABLE_NAMING_OTHER] += others
        }

        // Indentation
        fsIndentation[email]!![FactCodes.INDENTATION_SPACES] +=
            lines.count { it.isNotBlank() && it.startsWith(" ") && !it.contains("\t") }
        fsIndentation[email]!![FactCodes.INDENTATION_TABS] +=
            lines.count { it.startsWith("\t") }
    }

    override fun calculateFacts(repoRehash: String): List<Fact> {
        val fs = mutableListOf<Fact>()
        emails.forEach { email ->
            val authorEmail = Email(email)
            fsDayTime[email]?.forEachIndexed { hour, count ->
                if (count > 0) {
                    fs.add(Fact(repoRehash, FactCodes.COMMIT_DAY_TIME, hour,
                        count.toString(), authorEmail))
                }
            }
            fsDayWeek[email]?.forEachIndexed { day, count ->
                if (count > 0) {
                    fs.add(Fact(repoRehash, FactCodes.COMMIT_DAY_WEEK, day,
                        count.toString(), authorEmail))
                }
            }
            fsVariableNaming[email]?.forEachIndexed { naming, count ->
                if (count > 0) {
                    fs.add(Fact(repoRehash, FactCodes.VARIABLE_NAMING, naming,
                        count.toString(), authorEmail))
                }
            }
            fsIndentation[email]?.forEachIndexed { indentation, count ->
                if (count > 0) {
                    fs.add(Fact(repoRehash, FactCodes.INDENTATION, indentation,
                        count.toString(), authorEmail))
                }
            }

            fs.add(Fact(repoRehash, FactCodes.REPO_DATE_START, 0,
                fsRepoDateStart[email].toString(), authorEmail))
            fs.add(Fact(repoRehash, FactCodes.REPO_DATE_END, 0,
                fsRepoDateEnd[email].toString(), authorEmail))
            fs.add(Fact(repoRehash, FactCodes.COMMIT_NUM, 0,
                fsCommitNum[email].toString(), authorEmail))
            fs.add(Fact(repoRehash, FactCodes.COMMIT_LINE_NUM_AVG, 0,
                fsCommitLineNumAvg[email].toString(), authorEmail))
            fs.add(Fact(repoRehash, FactCodes.LINE_NUM, 0,
                fsLineNum[email].toString(), authorEmail))
            fs.add(Fact(repoRehash, FactCodes.LINE_LEN_AVG, 0,
                fsLineLenAvg[email].toString(), authorEmail))
        }
        return fs
    }

    private fun calcIncAvg(prev: Double, element: Double, count: Long): Double {
        return prev * (1 - 1.0 / count) + element / count
    }
}
