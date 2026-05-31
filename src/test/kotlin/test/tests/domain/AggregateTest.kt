package test.tests.domain

import app.domain.repository.aggregate.RepoAggregate
import app.domain.repository.entity.Author
import app.domain.repository.entity.Repo
import app.domain.repository.factory.CommitFactory
import app.domain.shared.valueobject.Email
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AggregateTest : Spek({
    given("RepoAggregate") {
        val repo = Repo(rehash = "test-rehash")

        it("registers authors and deduplicates by email") {
            val aggregate = RepoAggregate(repo)
            val author1 = Author(email = Email("test@domain.com"), name = "T")
            val author2 = Author(email = Email("test@domain.com"), name = "Test User")

            aggregate.registerAuthor(author1)
            aggregate.registerAuthor(author2)

            assertEquals(1, aggregate.authors.size)
            // Name should be updated to the more complete one
            val registered = aggregate.authors.first()
            assertEquals("Test User", registered.name)
        }

        it("finds first overlapping commit rehash") {
            val aggregate = RepoAggregate(repo)
            val commits = listOf(
                CommitFactory.create(
                    rehash = "a".repeat(64),
                    authorName = "Test", authorEmail = "t@d.com",
                    dateTimestamp = 1000, dateTimeZoneOffset = 0,
                    treeRehash = "b".repeat(64)
                ),
                CommitFactory.create(
                    rehash = "c".repeat(64),
                    authorName = "Test", authorEmail = "t@d.com",
                    dateTimestamp = 2000, dateTimeZoneOffset = 0,
                    treeRehash = "d".repeat(64)
                )
            )
            repo.loadServerHistory(commits)

            val localRehashes = listOf("x".repeat(64), "c".repeat(64), "a".repeat(64))
            val overlap = aggregate.findFirstOverlappingCommitRehash(localRehashes)
            assertEquals("c".repeat(64), overlap)
        }

        it("returns null when no overlap") {
            val repoNoHistory = Repo(rehash = "no-history")
            repoNoHistory.loadServerHistory(listOf())
            val aggregate = RepoAggregate(repoNoHistory)

            val localRehashes = listOf("x".repeat(64))
            val overlap = aggregate.findFirstOverlappingCommitRehash(localRehashes)
            assertNull(overlap)
        }

        it("filters known emails") {
            val aggregate = RepoAggregate(repo)
            repo.defineEmailFilter(listOf("server@domain.com"))

            val allEmails = setOf("a@d.com", "b@d.com", "server@domain.com", "user@d.com")
            val userEmails = setOf("user@d.com")

            val filtered = aggregate.filterKnownEmails(allEmails, userEmails)
            assertTrue(filtered.contains("server@domain.com"))
            assertTrue(filtered.contains("user@d.com"))
            assertEquals(2, filtered.size)
        }
    }
})
