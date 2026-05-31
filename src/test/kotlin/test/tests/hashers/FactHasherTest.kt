package test.tests.hashers

import app.domain.repository.FactCodes
import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.factory.CommitFactory
import app.domain.repository.valueobject.DiffContent
import app.domain.repository.valueobject.DiffFile
import app.domain.repository.valueobject.DiffRange
import app.domain.repository.valueobject.ChangeType
import app.domain.shared.valueobject.Email
import app.infrastructure.extractor.DefaultCommitExtractionService
import app.infrastructure.extractor.DefaultFactCalculationService
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import test.utils.assertFactDouble
import test.utils.assertFactInt
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FactHasherTest : Spek({
    val authorEmail1 = "test1@domain.com"
    val authorEmail2 = "test2@domain.com"
    val author1 = Author(email = Email(authorEmail1), name = "Test1")
    val author2 = Author(email = Email(authorEmail2), name = "Test2")

    fun createCommitWithLines(
        author: Author,
        lines: List<String>,
        timestamp: Long,
        tzOffset: Int = 0
    ): Commit {
        val diffFile = DiffFile(
            path = "test.txt",
            changeType = ChangeType.ADD,
            new = DiffContent(lines, listOf(DiffRange(0, lines.size)))
        )
        val commit = CommitFactory.create(
            rehash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(
                UUID.randomUUID().toString()
            ),
            authorName = author.name,
            authorEmail = author.email.value(),
            dateTimestamp = timestamp,
            dateTimeZoneOffset = tzOffset,
            treeRehash = org.apache.commons.codec.digest.DigestUtils.sha256Hex("tree"),
            diffs = listOf(diffFile)
        )
        return commit
    }

    fun createDate(year: Int = 2017, month: Int = 1, day: Int = 1,
                   hour: Int = 0, minute: Int = 0, seconds: Int = 0): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(year, month - 1, day, hour, minute, seconds)
        return cal.timeInMillis / 1000
    }

    given("commits for date facts") {
        val emails = hashSetOf(authorEmail1, authorEmail2)
        val extractionService = DefaultCommitExtractionService()

        it("sends day time and day week facts") {
            val factService = DefaultFactCalculationService(
                emails = emails, totalCommits = 3, extractionService = extractionService
            )

            // Sunday at 13:00
            val commit1 = createCommitWithLines(
                author1, listOf("line1", "line2"),
                createDate(year = 2017, month = 1, day = 1, hour = 13)
            )
            factService.processCommit(commit1)

            val facts = factService.calculateFacts("rehash")

            // Day time = 13
            val dayTimeFact = facts.find {
                it.code == FactCodes.COMMIT_DAY_TIME && it.key == 13 &&
                    it.authorEmail.value() == authorEmail1
            }
            assertTrue(dayTimeFact != null)
            assertEquals("1", dayTimeFact!!.value)

            // Day week = 6 (Sunday, 0-indexed from Monday)
            val dayWeekFact = facts.find {
                it.code == FactCodes.COMMIT_DAY_WEEK && it.key == 6 &&
                    it.authorEmail.value() == authorEmail1
            }
            assertTrue(dayWeekFact != null)
            assertEquals("1", dayWeekFact!!.value)
        }

        it("accumulates facts for multiple commits") {
            val factService = DefaultFactCalculationService(
                emails = emails, totalCommits = 3, extractionService = extractionService
            )

            // Author1: Sunday 13:00
            val commit1 = createCommitWithLines(
                author1, listOf("line1"),
                createDate(year = 2017, month = 1, day = 1, hour = 13)
            )
            // Author2: Monday 18:00
            val commit2 = createCommitWithLines(
                author2, listOf("line2"),
                createDate(year = 2017, month = 1, day = 2, hour = 18)
            )
            // Author1: Monday 13:00
            val commit3 = createCommitWithLines(
                author1, listOf("line3"),
                createDate(year = 2017, month = 1, day = 2, hour = 13)
            )

            factService.processCommit(commit1)
            factService.processCommit(commit2)
            factService.processCommit(commit3)

            val facts = factService.calculateFacts("rehash")

            // Author1 has 2 commits at hour 13
            val author1DayTime = facts.find {
                it.code == FactCodes.COMMIT_DAY_TIME && it.key == 13 &&
                    it.authorEmail.value() == authorEmail1
            }
            assertTrue(author1DayTime != null)
            assertEquals("2", author1DayTime!!.value)

            // Author2 has 1 commit at hour 18
            val author2DayTime = facts.find {
                it.code == FactCodes.COMMIT_DAY_TIME && it.key == 18 &&
                    it.authorEmail.value() == authorEmail2
            }
            assertTrue(author2DayTime != null)
            assertEquals("1", author2DayTime!!.value)
        }
    }

    given("commits for repo date facts") {
        val emails = hashSetOf(authorEmail1)
        val extractionService = DefaultCommitExtractionService()

        it("tracks start and end dates") {
            val factService = DefaultFactCalculationService(
                emails = emails, totalCommits = 3, extractionService = extractionService
            )

            val startDate = createDate(year = 2016, month = 2, day = 10, hour = 13)
            val endDate = createDate(year = 2017, month = 4, day = 5, hour = 13)

            // Process in reverse order (as git log does - newest first)
            val commit1 = createCommitWithLines(author1, listOf("line1"), endDate)
            val commit2 = createCommitWithLines(author1, listOf("line2"), startDate)

            factService.processCommit(commit1)
            factService.processCommit(commit2)

            val facts = factService.calculateFacts("rehash")

            assertFactInt(FactCodes.REPO_DATE_START, 0,
                startDate.toInt(), authorEmail1, facts)
            assertFactInt(FactCodes.REPO_DATE_END, 0,
                endDate.toInt(), authorEmail1, facts)
        }
    }

    given("commits for naming convention facts") {
        val emails = hashSetOf(authorEmail1)
        val extractionService = DefaultCommitExtractionService()

        it("detects camelCase and snake_case") {
            val factService = DefaultFactCalculationService(
                emails = emails, totalCommits = 4, extractionService = extractionService
            )

            val lines = listOf("camelCase1", "camelCase2", "snake_case", "fn()")
            for (line in lines) {
                val commit = createCommitWithLines(
                    author1, listOf(line),
                    createDate(hour = 10)
                )
                factService.processCommit(commit)
            }

            val facts = factService.calculateFacts("rehash")

            assertFactInt(FactCodes.VARIABLE_NAMING,
                FactCodes.VARIABLE_NAMING_SNAKE_CASE, 1, authorEmail1, facts)
            assertFactInt(FactCodes.VARIABLE_NAMING,
                FactCodes.VARIABLE_NAMING_CAMEL_CASE, 2, authorEmail1, facts)
            assertFactInt(FactCodes.VARIABLE_NAMING,
                FactCodes.VARIABLE_NAMING_OTHER, 1, authorEmail1, facts)
        }
    }

    given("commits for indentation facts") {
        val emails = hashSetOf(authorEmail1)
        val extractionService = DefaultCommitExtractionService()

        it("detects tabs and spaces") {
            val factService = DefaultFactCalculationService(
                emails = emails, totalCommits = 5, extractionService = extractionService
            )

            val lines = listOf("\tdef test()", "\t\tdef fn()", "a b c d", "    ",
                "    def fn()")
            for (line in lines) {
                val commit = createCommitWithLines(
                    author1, listOf(line),
                    createDate(hour = 10)
                )
                factService.processCommit(commit)
            }

            val facts = factService.calculateFacts("rehash")

            assertFactInt(FactCodes.INDENTATION,
                FactCodes.INDENTATION_TABS, 2, authorEmail1, facts)
            assertFactInt(FactCodes.INDENTATION,
                FactCodes.INDENTATION_SPACES, 1, authorEmail1, facts)
        }
    }

    given("commits for commit num and line stats") {
        val emails = hashSetOf(authorEmail1)
        val extractionService = DefaultCommitExtractionService()

        it("calculates commit count and line averages") {
            val factService = DefaultFactCalculationService(
                emails = emails, totalCommits = 3, extractionService = extractionService
            )

            val commit1 = createCommitWithLines(
                author1, listOf("line1", "line2", "line3"),
                createDate(hour = 10)
            )
            val commit2 = createCommitWithLines(
                author1, listOf("line4", "line5", "line6"),
                createDate(hour = 11)
            )
            val commit3 = createCommitWithLines(
                author1, listOf("line7", "line8", "line9"),
                createDate(hour = 12)
            )

            factService.processCommit(commit1)
            factService.processCommit(commit2)
            factService.processCommit(commit3)

            val facts = factService.calculateFacts("rehash")

            assertFactInt(FactCodes.COMMIT_NUM, 0, 3, authorEmail1, facts)
            assertFactInt(FactCodes.LINE_NUM, 0, 9, authorEmail1, facts)
        }
    }
})
