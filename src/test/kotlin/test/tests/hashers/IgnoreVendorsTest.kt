package test.tests.hashers

import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.valueobject.CommitStats
import app.domain.shared.valueobject.Email
import app.infrastructure.extractor.DefaultCommitExtractionService
import app.infrastructure.git.JGitRepositoryAdapter
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import test.utils.TestRepo
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IgnoreVendorsTest : Spek({
    val userName = "Contributor"
    val userEmail = "test@domain.com"
    val extractionService = DefaultCommitExtractionService()
    val gitAdapter = JGitRepositoryAdapter()

    given("vendor files should be ignored") {
        val author = Author(email = Email(userEmail), name = userName)
        val testRepoPath = "../testrepo-ddd-ignore-vendors"
        val testRepo = TestRepo(testRepoPath)

        it("ignores .min.js files but processes normal .js files") {
            val lines = listOf("let i = 0")

            testRepo.createFile("lala.js", lines)
            testRepo.createFile("pdf.worker.min.js", lines)
            testRepo.commit(message = "commit1", author = author)

            gitAdapter.open(testRepoPath)
            val (rehashes, _, _) = gitAdapter.fetchRehashesAndAuthors()

            val processedCommits = mutableListOf<Commit>()
            val commitStream = gitAdapter.crawlCommits(
                repoRehash = "test-rehash",
                totalCommitCount = rehashes.size,
                filteredEmails = setOf(userEmail)
            )

            commitStream.subscribe(
                onNext = { commit ->
                    val stats = extractionService.extractStats(commit.diffs)
                    commit.registerStats(stats)
                    processedCommits.add(commit)
                },
                onError = { e -> throw e },
                onComplete = {}
            )

            gitAdapter.close()

            val allStats = processedCommits.flatMap { it.stats }
            // Only lala.js should be counted (1 line), pdf.worker.min.js ignored
            val jsStats = allStats.filter { it.tech == "javascript" }
            assertTrue(jsStats.isNotEmpty())
            assertEquals(1, jsStats.sumBy { it.numLinesAdded })
        }

        afterGroup {
            testRepo.destroy()
        }
    }
})
