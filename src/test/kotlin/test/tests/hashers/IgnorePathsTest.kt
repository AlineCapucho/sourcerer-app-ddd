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

class IgnorePathsTest : Spek({
    val userName = "Contributor"
    val userEmail = "test@domain.com"
    val extractionService = DefaultCommitExtractionService()
    val gitAdapter = JGitRepositoryAdapter()

    given("commits with .sourcerer-conf ignore paths") {
        val author = Author(email = Email(userEmail), name = userName)
        val testRepoPath = "../testrepo-ddd-ignore-paths"
        val testRepo = TestRepo(testRepoPath)

        it("ignores files specified in .sourcerer-conf") {
            val lines = listOf("x = 1", "y = 2", "z = 3")

            // Commit 1: add test.py (should be counted)
            testRepo.createFile("test.py", lines)
            testRepo.commit(message = "commit1", author = author)

            // Commit 2: add ignore.py (should be ignored after config)
            testRepo.createFile("ignore.py", lines)
            testRepo.commit(message = "commit2", author = author)

            // Commit 3: add .sourcerer-conf that ignores ignore.py
            testRepo.createFile(".sourcerer-conf",
                listOf("[ignore]", "ignore.py"))
            testRepo.commit(message = "commit3 - add config", author = author)

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

            // Only test.py should have python stats (ignore.py is ignored)
            val allStats = processedCommits.flatMap { it.stats }
            val pyStats = allStats.filter { it.tech == "python" }
            // test.py has 3 lines added
            assertEquals(3, pyStats.sumBy { it.numLinesAdded })
        }

        afterGroup {
            testRepo.destroy()
        }
    }
})
