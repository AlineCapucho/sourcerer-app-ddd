package test.tests.hashers

import app.domain.repository.entity.Author
import app.domain.shared.valueobject.Email
import app.infrastructure.extractor.DefaultAuthorDistanceService
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthorDistanceHasherTest : Spek({
    given("repo with multiple authors modifying same files") {
        val author1Email = "first.author@gmail.com"
        val author2Email = "second.author@gmail.com"
        val author3Email = "third.author@gmail.com"
        val allEmails = setOf(author1Email, author2Email, author3Email)
        val userEmails = setOf(author2Email)
        val repoRehash = "test_repo_rehash"

        it("calculates distance based on shared file paths") {
            val service = DefaultAuthorDistanceService(allEmails, userEmails)

            // Author1 and Author2 modify the same file
            service.registerContribution(Email(author1Email),
                listOf("test1.txt"), 1000)
            service.registerContribution(Email(author2Email),
                listOf("test1.txt"), 2000)

            // Author3 modifies a different file
            service.registerContribution(Email(author3Email),
                listOf("other.txt"), 3000)

            val distances = service.calculateDistances(repoRehash)

            // Author2 (user) should have high distance to Author1 (same file)
            val distToAuthor1 = distances.find { it.email.value() == author1Email }
            assertTrue(distToAuthor1 != null)
            assertEquals(1.0, distToAuthor1!!.score)

            // Author2 (user) should have zero distance to Author3 (different file)
            val distToAuthor3 = distances.find { it.email.value() == author3Email }
            assertTrue(distToAuthor3 != null)
            assertEquals(0.0, distToAuthor3!!.score)
        }

        it("calculates partial overlap") {
            val service = DefaultAuthorDistanceService(allEmails, userEmails)

            // Author1 modifies file1 and file2
            service.registerContribution(Email(author1Email),
                listOf("file1.txt", "file2.txt"), 1000)

            // Author2 (user) modifies file1 and file3
            service.registerContribution(Email(author2Email),
                listOf("file1.txt", "file3.txt"), 2000)

            val distances = service.calculateDistances(repoRehash)

            // Jaccard: intersection={file1} / union={file1,file2,file3} = 1/3
            val distToAuthor1 = distances.find { it.email.value() == author1Email }
            assertTrue(distToAuthor1 != null)
            assertTrue(Math.abs(1.0 / 3.0 - distToAuthor1!!.score) < 0.01)
        }

        it("handles empty contributions") {
            val service = DefaultAuthorDistanceService(allEmails, userEmails)

            // Only user contributes
            service.registerContribution(Email(author2Email),
                listOf("file1.txt"), 1000)

            val distances = service.calculateDistances(repoRehash)
            assertTrue(distances.isEmpty())
        }
    }
})
