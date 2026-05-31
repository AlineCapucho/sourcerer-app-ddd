package app.infrastructure.git

import app.domain.repository.entity.Commit
import app.domain.repository.factory.CommitFactory
import app.domain.repository.port.CommitStream
import app.infrastructure.Logger
import org.apache.commons.codec.digest.DigestUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.util.io.DisabledOutputStream

/**
 * Implementação de CommitStream usando JGit.
 * Adapta o crawling de commits JGit para a interface do domínio.
 */
class JGitCommitStream(
    private val git: Git,
    private val adapter: JGitRepositoryAdapter,
    private val repoRehash: String,
    private val totalCommitCount: Int,
    private val filteredEmails: Set<String>?
) : CommitStream {

    private val CONF_FILE_PATH = ".sourcerer-conf"
    private val MAX_DIFF_SIZE = 600000

    override fun subscribe(
        onNext: (Commit) -> Unit,
        onError: (Throwable) -> Unit,
        onComplete: () -> Unit
    ) {
        try {
            val repo = git.repository
            val revWalk = RevWalk(repo)
            val head = revWalk.parseCommit(adapter.getDefaultBranchHead(git))

            val df = DiffFormatter(DisabledOutputStream.INSTANCE)
            df.setRepository(repo)
            df.isDetectRenames = true

            val confTreeWalk = TreeWalk(repo)
            confTreeWalk.addTree(head.tree)
            confTreeWalk.filter = PathFilter.create(CONF_FILE_PATH)

            var ignoredPaths = if (confTreeWalk.next()) {
                adapter.getIgnoredPaths(repo, confTreeWalk.getObjectId(0))
            } else listOf()

            var commitCount = 0
            revWalk.markStart(head)
            var commit: RevCommit? = revWalk.next()

            while (commit != null) {
                commitCount++
                val parentCommit: RevCommit? = revWalk.next()

                val perc = if (totalCommitCount != 0) {
                    (commitCount.toDouble() / totalCommitCount) * 100
                } else 0.0
                Logger.printCommit(commit.shortMessage, commit.name, perc)

                val email = commit.authorIdent.emailAddress.toLowerCase()
                if (filteredEmails != null && !filteredEmails.contains(email)) {
                    commit = parentCommit
                    continue
                }

                val diffEntries = df.scan(parentCommit, commit)
                    .filter { diff -> diff.changeType != DiffEntry.ChangeType.COPY }
                    .filter { diff ->
                        val path = diff.newPath
                        for (cnv in VendorConventions) {
                            if (cnv.containsMatchIn(path) || cnv.containsMatchIn(diff.oldPath)) {
                                return@filter false
                            }
                        }
                        val fileId = if (path != DiffEntry.DEV_NULL) {
                            diff.newId.toObjectId()
                        } else {
                            diff.oldId.toObjectId()
                        }
                        val stream = try { repo.open(fileId).openStream() }
                                     catch (e: Exception) { null }
                        stream != null && !RawText.isBinary(stream)
                    }
                    .filter { diff ->
                        val filePath = if (diff.newPath != DiffEntry.DEV_NULL) {
                            diff.newPath
                        } else { diff.oldPath }

                        if (diff.oldPath == CONF_FILE_PATH) {
                            ignoredPaths = adapter.getIgnoredPaths(repo, diff.newId.toObjectId())
                        }

                        !ignoredPaths.any { path ->
                            if (path.endsWith("/")) filePath.startsWith(path)
                            else path == filePath
                        }
                    }

                val jgitDiffs = diffEntries.map { diff ->
                    JGitDiff(diff, df.toFileHeader(diff).toEditList())
                }.filter { diff ->
                    diff.editList.fold(0) { acc, edit ->
                        acc + edit.lengthA + edit.lengthB
                    } < MAX_DIFF_SIZE
                }

                val diffs = adapter.getDiffFiles(repo, jgitDiffs)
                val coauthors = adapter.getCoauthors(commit.fullMessage)

                val domainCommit = CommitFactory.create(
                    rehash = DigestUtils.sha256Hex(commit.id.name),
                    authorName = commit.authorIdent.name,
                    authorEmail = email,
                    dateTimestamp = commit.authorIdent.getWhen().time / 1000,
                    dateTimeZoneOffset = commit.authorIdent.timeZoneOffset,
                    treeRehash = DigestUtils.sha256Hex(commit.tree.name),
                    coauthors = coauthors.map { it.name to it.email.value() },
                    diffs = diffs,
                    repoRehash = repoRehash
                )

                onNext(domainCommit)
                commit = parentCommit
            }

            onComplete()
        } catch (e: Throwable) {
            onError(e)
        }
    }
}
