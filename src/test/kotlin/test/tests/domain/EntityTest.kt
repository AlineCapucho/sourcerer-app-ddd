package test.tests.domain

import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.entity.Repo
import app.domain.repository.factory.CommitFactory
import app.domain.repository.valueobject.*
import app.domain.shared.valueobject.Email
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class EntityTest : Spek({
    given("Author entity") {
        it("identity is based on email") {
            val author1 = Author(email = Email("test@domain.com"), name = "Test")
            val author2 = Author(email = Email("test@domain.com"), name = "Other Name")
            assertEquals(author1, author2)
        }

        it("different emails are different authors") {
            val author1 = Author(email = Email("a@domain.com"), name = "A")
            val author2 = Author(email = Email("b@domain.com"), name = "B")
            assertNotEquals(author1, author2)
        }

        it("updates name if more complete") {
            val author = Author(email = Email("test@domain.com"), name = "J")
            author.updateNameIfMoreComplete("John Doe")
            assertEquals("John Doe", author.name)
        }

        it("does not update name if shorter") {
            val author = Author(email = Email("test@domain.com"), name = "John Doe")
            author.updateNameIfMoreComplete("J")
            assertEquals("John Doe", author.name)
        }

        it("associates to repo") {
            val author = Author(email = Email("test@domain.com"), name = "Test")
            author.associateToRepo("rehash123")
            assertEquals("rehash123", author.repoRehash)
        }
    }

    given("Commit entity") {
        it("identity is based on rehash") {
            val commit1 = CommitFactory.create(
                rehash = "abc123" + "0".repeat(58),
                authorName = "Test",
                authorEmail = "test@domain.com",
                dateTimestamp = 1000,
                dateTimeZoneOffset = 0,
                treeRehash = "tree" + "0".repeat(60)
            )
            val commit2 = CommitFactory.create(
                rehash = "abc123" + "0".repeat(58),
                authorName = "Other",
                authorEmail = "other@domain.com",
                dateTimestamp = 2000,
                dateTimeZoneOffset = 0,
                treeRehash = "tree" + "0".repeat(60)
            )
            assertEquals(commit1, commit2)
        }

        it("validates non-blank rehash") {
            assertFailsWith<IllegalArgumentException> {
                Commit(
                    rehash = "",
                    author = Author(email = Email("test@domain.com"), name = "Test"),
                    dateTimestamp = 1000
                )
            }
        }

        it("registers diffs and calculates line counts") {
            val commit = CommitFactory.create(
                rehash = "a".repeat(64),
                authorName = "Test",
                authorEmail = "test@domain.com",
                dateTimestamp = 1000,
                dateTimeZoneOffset = 0,
                treeRehash = "b".repeat(64)
            )

            val diffs = listOf(DiffFile(
                path = "test.py",
                changeType = ChangeType.ADD,
                new = DiffContent(
                    listOf("line1", "line2", "line3"),
                    listOf(DiffRange(0, 3))
                )
            ))

            commit.registerDiffs(diffs)
            assertEquals(3, commit.numLinesAdded)
            assertEquals(0, commit.numLinesDeleted)
            assertEquals(listOf("line1", "line2", "line3"), commit.getAllAdded())
        }

        it("copies for coauthor preserves stats") {
            val commit = CommitFactory.create(
                rehash = "a".repeat(64),
                authorName = "Author1",
                authorEmail = "author1@domain.com",
                dateTimestamp = 1000,
                dateTimeZoneOffset = 0,
                treeRehash = "b".repeat(64)
            )
            commit.registerStats(listOf(
                CommitStats(numLinesAdded = 5, type = CommitStats.TYPE_LANGUAGE, tech = "python")
            ))

            val coauthor = Author(email = Email("coauthor@domain.com"), name = "Coauthor")
            val copy = commit.copyForCoauthor(coauthor)

            assertEquals(coauthor, copy.author)
            assertEquals(commit.stats, copy.stats)
            assertEquals(commit.rehash, copy.rehash)
        }
    }

    given("Repo entity") {
        it("validates non-blank rehash") {
            assertFailsWith<IllegalArgumentException> {
                Repo(rehash = "")
            }
        }

        it("identity is based on rehash") {
            val repo1 = Repo(rehash = "abc123")
            val repo2 = Repo(rehash = "abc123")
            assertEquals(repo1, repo2)
        }

        it("loads server history") {
            val repo = Repo(rehash = "test-rehash")
            val commits = listOf(
                CommitFactory.create(
                    rehash = "c".repeat(64),
                    authorName = "Test",
                    authorEmail = "test@domain.com",
                    dateTimestamp = 1000,
                    dateTimeZoneOffset = 0,
                    treeRehash = "d".repeat(64)
                )
            )
            repo.loadServerHistory(commits)
            assertEquals(1, repo.commits.size)
        }
    }
})
