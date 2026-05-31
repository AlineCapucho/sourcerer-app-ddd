package test.tests.hashers

import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.factory.CommitFactory
import app.domain.repository.valueobject.CommitStats
import app.domain.shared.valueobject.Email
import app.infrastructure.extractor.DefaultCommitExtractionService
import app.infrastructure.git.JGitRepositoryAdapter
import org.apache.commons.codec.digest.DigestUtils
import org.eclipse.jgit.api.Git
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import test.utils.TestRepo
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommitHasherTest : Spek({
    fun cleanRepos() {
        Runtime.getRuntime().exec("rm -rf ../tmp_repo_ddd").waitFor()
    }

    val userName = "First Contributor"
    val userEmail = "test@domain.com"
    val secondUserName = "Second Contributor"
    val secondUserEmail = "test2@domain.com"

    val extractionService = DefaultCommitExtractionService()
    val gitAdapter = JGitRepositoryAdapter()

    cleanRepos()

    given("repo with commits - extraction service") {
        val testRepoPath = "../testrepo-ddd-commit-hasher"
        val testRepo = TestRepo(testRepoPath)
        val author = Author(email = Email(userEmail), name = userName)

        it("extracts language stats from python files") {
            val lines = listOf("x = [i**2 for i in range(9999)]", "def fn()", "x = 1")
            for (i in 0..lines.size - 1) {
                val line = lines[i]
                val fileName = "file$i.py"
                testRepo.createFile(fileName, listOf(line))
                testRepo.commit(message = "$line in $fileName", author = author)
            }

            // Use the git adapter to crawl commits
            gitAdapter.open(testRepoPath)
            val (rehashes, authors, _) = gitAdapter.fetchRehashesAndAuthors()

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

            assertTrue(processedCommits.isNotEmpty())
            val allStats = processedCommits.flatMap { it.stats }
            val langStats = allStats.filter { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStats.all { it.tech == "python" })
            assertTrue(langStats.sumBy { it.numLinesAdded } > 0)
        }

        afterGroup {
            testRepo.destroy()
        }
    }

    given("repo with typescript files") {
        val testRepoPath = "../testrepo-ddd-typescript"
        val testRepo = TestRepo(testRepoPath)
        val author = Author(email = Email(userEmail), name = userName)

        it("detects typescript language") {
            val lines = listOf("const x: number = 1;", "export default x;")
            for (i in 0..lines.size - 1) {
                testRepo.createFile("file$i.ts", listOf(lines[i]))
                testRepo.commit(message = "add ts file $i", author = author)
            }

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
            val langStats = allStats.filter { it.type == CommitStats.TYPE_LANGUAGE }
            assertTrue(langStats.all { it.tech == "typescript" })
            assertEquals(2, langStats.sumBy { it.numLinesAdded })
        }

        afterGroup {
            testRepo.destroy()
        }
    }

    given("commit with multiple authors (coauthors)") {
        val testRepoPath = "../testrepo-ddd-coauthors"
        val testRepo = TestRepo(testRepoPath)
        val author1 = Author(email = Email(userEmail), name = userName)
        val author2 = Author(email = Email(secondUserEmail), name = secondUserName)

        it("detects coauthors from commit message") {
            testRepo.createFile("file.txt", listOf("line 1"))
            val message = "add file\n\nCo-authored-by: $secondUserName <$secondUserEmail>"
            testRepo.commit(message = message, author = author1)

            gitAdapter.open(testRepoPath)
            val (rehashes, _, _) = gitAdapter.fetchRehashesAndAuthors()

            val processedCommits = mutableListOf<Commit>()
            val commitStream = gitAdapter.crawlCommits(
                repoRehash = "test-rehash",
                totalCommitCount = rehashes.size,
                filteredEmails = setOf(userEmail, secondUserEmail)
            )

            commitStream.subscribe(
                onNext = { commit ->
                    processedCommits.add(commit)
                    // Add coauthor copies
                    commit.coauthors.forEach { coauthor ->
                        processedCommits.add(commit.copyForCoauthor(coauthor))
                    }
                },
                onError = { e -> throw e },
                onComplete = {}
            )

            gitAdapter.close()

            val allAuthors = processedCommits.map { it.author.email.value() }.toHashSet()
            assertTrue(userEmail in allAuthors)
            assertTrue(secondUserEmail in allAuthors)
        }

        afterGroup {
            testRepo.destroy()
        }
    }

    cleanRepos()
})
