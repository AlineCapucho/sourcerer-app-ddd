package app.infrastructure.git

import app.application.usecase.EmptyRepoException
import app.application.usecase.GitRepositoryValidator
import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.factory.CommitFactory
import app.domain.repository.port.*
import app.domain.repository.valueobject.*
import app.domain.shared.valueobject.Email
import app.infrastructure.Logger
import io.reactivex.Observable
import org.apache.commons.codec.digest.DigestUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.LinkedList

/**
 * ACL: Adaptador JGit que implementa GitRepositoryPort.
 * Isola o domínio de detalhes de implementação do JGit.
 */
class JGitRepositoryAdapter : GitRepositoryPort, GitRepositoryValidator {

    private var git: Git? = null
    private val REMOTE_HEAD = "refs/remotes/origin/HEAD"
    private val REMOTE_MASTER_BRANCH = "refs/remotes/origin/master"
    private val LOCAL_MASTER_BRANCH = "refs/heads/master"
    private val LOCAL_HEAD = "HEAD"
    private val REFS = listOf(REMOTE_HEAD, REMOTE_MASTER_BRANCH,
        LOCAL_MASTER_BRANCH, LOCAL_HEAD)
    private val CONF_FILE_PATH = ".sourcerer-conf"
    private val MAX_DIFF_SIZE = 600000
    private val coauthoredRegex = Regex("""Co-authored-by: (.+) <(.+)>""")

    override fun open(path: String) {
        git = Git.open(File(path))
    }

    override fun close() {
        git?.repository?.close()
        git?.close()
        git = null
    }

    override fun isValidRepo(path: String): Boolean {
        return try {
            val file = File(path)
            if (!file.isDirectory) return false
            val g = Git.open(file)
            val commitId = getDefaultBranchHead(g)
            g.repository?.close()
            g.close()
            commitId != null
        } catch (e: Exception) {
            false
        }
    }

    override fun isValidGitRepo(path: String): Boolean {
        val file = File(path)
        return file.isDirectory && file.name.equals(".git", ignoreCase = true)
    }

    override fun fetchRehashesAndAuthors(): Triple<List<String>, Set<Author>, Map<String, Int>> {
        val g = git ?: throw IllegalStateException("Git repository not opened")
        val head = RevWalk(g.repository).parseCommit(getDefaultBranchHead(g))

        val revWalk = RevWalk(g.repository)
        revWalk.markStart(head)

        val commitsRehashes = LinkedList<String>()
        val emails = hashSetOf<String>()
        val names = hashMapOf<String, String>()
        val commitsCount = hashMapOf<String, Int>()
        val coauthorsList = mutableListOf<Author>()

        var commit: RevCommit? = revWalk.next()
        while (commit != null) {
            commitsRehashes.add(DigestUtils.sha256Hex(commit.name))
            val email = commit.authorIdent.emailAddress.toLowerCase()
            val name = commit.authorIdent.name
            if (!emails.contains(email)) {
                emails.add(email)
                names[email] = name
            } else {
                if (name.length > names[email]!!.length) {
                    names[email] = name
                }
            }
            val coauthors = getCoauthors(commit.fullMessage)
            coauthorsList.addAll(coauthors)

            commitsCount[email] = commitsCount.getOrDefault(email, 0) + 1

            commit.disposeBody()
            commit = revWalk.next()
        }
        revWalk.dispose()

        val authors = emails.map { email ->
            Author(email = Email(email), name = names[email]!!)
        }.toHashSet()
        authors.addAll(coauthorsList)

        return Triple(commitsRehashes, authors, commitsCount)
    }

    override fun crawlCommits(
        repoRehash: String,
        totalCommitCount: Int,
        filteredEmails: Set<String>?
    ): CommitStream {
        val g = git ?: throw IllegalStateException("Git repository not opened")
        return JGitCommitStream(g, this, repoRehash, totalCommitCount, filteredEmails)
    }

    override fun crawlPaths(): PathStream {
        val g = git ?: throw IllegalStateException("Git repository not opened")
        return JGitPathStream(g, this)
    }

    override fun getRemoteOrigin(): String {
        val g = git ?: return ""
        return g.repository.config.getString("remote", "origin", "url") ?: ""
    }

    override fun getUserName(): String {
        val g = git ?: return ""
        return g.repository.config.getString("user", null, "name") ?: ""
    }

    override fun getUserEmail(): String {
        val g = git ?: return ""
        return g.repository.config.getString("user", null, "email") ?: ""
    }

    fun getDefaultBranchHead(g: Git): ObjectId {
        for (ref in REFS) {
            val branch = g.repository.resolve(ref) ?: continue
            Logger.debug { "Hashing from $ref" }
            return branch
        }
        throw EmptyRepoException("No remote default, master or HEAD found")
    }

    fun getDiffFiles(jgitRepo: Repository, jgitDiffs: List<JGitDiff>): List<DiffFile> {
        return jgitDiffs.mapNotNull { (diff, edits) ->
            val new = try { getContentByObjectId(jgitRepo, diff.newId.toObjectId()) }
                      catch (e: Exception) { Logger.error(e); null }
            val old = try { getContentByObjectId(jgitRepo, diff.oldId.toObjectId()) }
                      catch (e: Exception) { Logger.error(e); null }

            if (new != null && old != null) {
                val path = when (diff.changeType) {
                    DiffEntry.ChangeType.DELETE -> diff.oldPath
                    else -> diff.newPath
                }
                val changeType = mapChangeType(diff.changeType)
                DiffFile(
                    path = path,
                    changeType = changeType,
                    old = DiffContent(old, edits.map { edit ->
                        DiffRange(edit.beginA, edit.endA)
                    }),
                    new = DiffContent(new, edits.map { edit ->
                        DiffRange(edit.beginB, edit.endB)
                    })
                )
            } else null
        }
    }

    private fun mapChangeType(jgitType: DiffEntry.ChangeType): ChangeType {
        return when (jgitType) {
            DiffEntry.ChangeType.ADD -> ChangeType.ADD
            DiffEntry.ChangeType.MODIFY -> ChangeType.MODIFY
            DiffEntry.ChangeType.DELETE -> ChangeType.DELETE
            DiffEntry.ChangeType.RENAME -> ChangeType.RENAME
            DiffEntry.ChangeType.COPY -> ChangeType.COPY
        }
    }

    private fun getContentByObjectId(repo: Repository, objectId: ObjectId): List<String> {
        return try {
            val obj = repo.open(objectId)
            val rawText = RawText(obj.bytes)
            val content = ArrayList<String>(rawText.size())
            for (i in 0..(rawText.size() - 1)) {
                content.add(rawText.getString(i))
            }
            content
        } catch (e: Exception) {
            listOf()
        }
    }

    fun getCoauthors(message: String): List<Author> {
        val coauthorsResult = coauthoredRegex.findAll(message)
        return coauthorsResult.map { result ->
            val name = result.groupValues[1]
            val email = result.groupValues[2].toLowerCase()
            Author(email = Email(email), name = name)
        }.toList()
    }

    fun getIgnoredPaths(repo: Repository, objectId: ObjectId?): List<String> {
        return try {
            if (objectId == null) return listOf()
            val list = mutableListOf<String>()
            val fileLoader = repo.open(objectId)
            val reader = BufferedReader(InputStreamReader(fileLoader.openStream()))
            var collectIgnored = false
            for (line in reader.lines()) {
                if (line == "" || line.startsWith("#")) continue
                if (line.startsWith("[")) {
                    collectIgnored = (line == "[ignore]")
                    continue
                }
                if (collectIgnored) list.add(line)
            }
            list
        } catch (e: Exception) { listOf() }
    }
}

data class JGitDiff(val diffEntry: DiffEntry, val editList: org.eclipse.jgit.diff.EditList)
