package app.infrastructure.git

import app.domain.repository.port.PathData
import app.domain.repository.port.PathStream
import app.infrastructure.Logger
import org.apache.commons.codec.digest.DigestUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.util.io.DisabledOutputStream

/**
 * Implementação de PathStream usando JGit.
 * Adapta o crawling de caminhos para cálculo de distância entre autores.
 */
class JGitPathStream(
    private val git: Git,
    private val adapter: JGitRepositoryAdapter
) : PathStream {

    override fun subscribe(
        onNext: (PathData) -> Unit,
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

            revWalk.markStart(head)
            var commit: RevCommit? = revWalk.next()

            while (commit != null) {
                val parentCommit: RevCommit? = revWalk.next()
                val email = commit.authorIdent.emailAddress.toLowerCase()
                val timestamp = commit.authorIdent.getWhen().time / 1000

                val diffEntries = df.scan(parentCommit, commit)
                val paths = diffEntries.map { diff ->
                    if (diff.newPath != DiffEntry.DEV_NULL) diff.newPath
                    else diff.oldPath
                }

                if (paths.isNotEmpty()) {
                    onNext(PathData(email = email, paths = paths, timestamp = timestamp))
                }

                commit = parentCommit
            }

            onComplete()
        } catch (e: Throwable) {
            onError(e)
        }
    }
}
