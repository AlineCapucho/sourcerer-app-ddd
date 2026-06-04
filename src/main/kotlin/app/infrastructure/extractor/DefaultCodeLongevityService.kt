package app.infrastructure.extractor

import app.domain.repository.FactCodes
import app.domain.repository.service.CodeLongevityService
import app.domain.repository.valueobject.Fact
import app.domain.shared.valueobject.Email
import app.infrastructure.Logger
import app.infrastructure.config.FileHelper
import app.infrastructure.git.JGitRepositoryAdapter
import io.reactivex.Observable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Representa uma linha de código em uma revisão de arquivo.
 */
class RevCommitLine(
    val commit: RevCommit,
    val fileId: AnyObjectId,
    val file: String,
    val line: Int,
    val isDeleted: Boolean
) {
    val id: String
        get() = "${fileId.name}:$line"
}

/**
 * Representa uma linha de código na história do repositório.
 * Conecta a revisão de origem (from) com a revisão de destino (to).
 */
class CodeLine(
    val repo: Repository,
    val from: RevCommitLine,
    val to: RevCommitLine
) {
    /**
     * Id da linha na revisão em que foi adicionada.
     */
    val oldId: String
        get() = from.id

    /**
     * Id da linha na revisão em que foi deletada ou no HEAD.
     */
    val newId: String
        get() = to.id

    /**
     * Idade da linha em segundos.
     */
    var age: Long = 0
        get() {
            if (field == 0L) {
                field = (to.commit.commitTime - from.commit.commitTime).toLong()
            }
            return field
        }

    /**
     * Texto da linha de código.
     */
    val text: String
        get() = RawText(repo.open(from.fileId).bytes).getString(from.line)

    /**
     * Email do autor da linha.
     */
    val authorEmail: String
        get() = from.commit.authorIdent.emailAddress

    /**
     * Email do editor da linha (quem a deletou).
     */
    val editorEmail: String?
        get() = if (isDeleted) to.commit.authorIdent.emailAddress else null

    /**
     * Data em que a linha foi alterada.
     */
    val editDate: Date
        get() = Date(to.commit.commitTime.toLong() * 1000)

    /**
     * Verdadeiro se a linha foi deletada.
     */
    val isDeleted: Boolean
        get() = to.isDeleted

    override fun toString(): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm z")
        val fd = df.format(Date(from.commit.commitTime.toLong() * 1000))
        val td = df.format(Date(to.commit.commitTime.toLong() * 1000))
        val fc = "${from.commit.name} '${from.commit.shortMessage}'"
        val tc = "${to.commit.name} '${to.commit.shortMessage}'"
        val revState = if (isDeleted) "deleted in" else "last known as"
        val state = if (isDeleted) "deleted" else "alive"
        return "Line '$text' - '${from.file}:${from.line}' added in $fc $fd\n" +
            "  $revState '${to.file}:${to.line}' in $tc $td,\n" +
            "  age: $age s - $state"
    }
}

/**
 * Estrutura de dados para armazenar informações de idade de linhas de código.
 * Serializável para persistência incremental em disco.
 */
class CodeLineAges : Serializable, Cloneable {
    /**
     * Par (soma de idades, contagem de linhas) representando idades agregadas.
     */
    data class AggrAge(var sum: Long = 0L, var count: Int = 0) : Serializable

    /**
     * Informação de uma linha: par (idade, email).
     */
    data class LineInfo(var age: Long, var email: String) : Serializable

    /**
     * Idades agregadas de linhas deletadas por email de autor.
     */
    var aggrAges: HashMap<String, AggrAge> = hashMapOf()

    /**
     * Mapa de IDs de linhas existentes para suas idades na revisão.
     */
    var lastingLines: HashMap<String, LineInfo> = hashMapOf()

    public override fun clone(): CodeLineAges {
        val clone = CodeLineAges()
        aggrAges.forEach { (email, age) ->
            clone.aggrAges[email] = age.copy()
        }
        lastingLines.forEach { (id, line) ->
            clone.lastingLines[id] = line.copy()
        }
        return clone
    }
}

/**
 * Detecta colegas e sua "proximidade de trabalho" a partir de commits.
 * Colegas são pares de desenvolvedores que editam o código um do outro.
 */
class Colleagues(private val repoRehash: String) {
    // Mapa de <email1, email2> para mapa de <mês, tempo mínimo>.
    private val map: HashMap<Pair<String, String>,
        HashMap<String, Long>> = hashMapOf()

    fun collect(line: CodeLine) {
        val authorEmail = line.authorEmail
        val editorEmail = line.editorEmail
        if (editorEmail == null || authorEmail == editorEmail) {
            return
        }
        val emails = Pair(authorEmail, editorEmail)

        val dates = map.getOrPut(emails) { hashMapOf() }
        val month = SimpleDateFormat("yyyy-MM").format(line.editDate)

        Logger.trace { "collected colleague, age: ${line.age}" }
        val vicinity = dates.getOrPut(month) { line.age }
        if (vicinity > line.age) {
            dates[month] = line.age
        }
    }

    fun calculateFacts(): List<Fact> {
        val facts = mutableListOf<Fact>()
        val auxHash = hashSetOf<Pair<String, String>>()

        for ((pair, dates) in map) {
            val email1 = pair.first
            val email2 = pair.second
            if (auxHash.contains(Pair(email2, email1))) {
                continue
            }

            val min1 = dates.minByOrNull { (_, vicinity) -> vicinity } ?: continue
            val dates2 = map[Pair(email2, email1)]
            if (dates2 != null) {
                auxHash.add(Pair(email1, email2))

                val min2 = dates2.minByOrNull { (_, vicinity) -> vicinity } ?: continue
                val min: Long = if (min1.value < min2.value) min1.value else min2.value

                facts.add(
                    Fact(
                        repoRehash = repoRehash,
                        code = FactCodes.COLLEAGUES,
                        key = 0,
                        value = email1,
                        authorEmail = Email(email1),
                        value2 = email2,
                        value3 = min.toString()
                    )
                )
            }
        }
        return facts
    }
}


/**
 * Implementação do CodeLongevityService na camada de infraestrutura.
 *
 * Rastreia linhas de código individuais pela história do Git para computar:
 * - LINE_LONGEVITY: idade média das linhas de código por autor (em segundos)
 * - LINE_LONGEVITY_REPO: idade média de todas as linhas do repositório
 * - COLLEAGUES: pares de desenvolvedores que editam o código um do outro
 *
 * Vive inteiramente na camada de infraestrutura por depender
 * fortemente de JGit (RevCommit, TreeWalk, RawText, ObjectId, etc.).
 */
class DefaultCodeLongevityService : CodeLongevityService {

    override fun calculateLongevityFacts(
        repoRehash: String,
        repoPath: String,
        emails: HashSet<String>
    ): List<Fact> {
        val git = Git.open(File(repoPath))
        return try {
            val repo = git.repository
            val revWalk = RevWalk(repo)
            val adapter = JGitRepositoryAdapter()
            val head: RevCommit = try {
                revWalk.parseCommit(adapter.getDefaultBranchHead(git))
            } catch (e: Exception) {
                Logger.error(e, "No branch found for longevity calculation")
                return emptyList()
            }

            val dataPath = FileHelper.getPath("longevity", repoRehash)
            val colleagues = Colleagues(repoRehash)

            updateFromObservable(
                repo = repo,
                revWalk = revWalk,
                head = head,
                git = git,
                adapter = adapter,
                dataPath = dataPath,
                colleagues = colleagues,
                emails = emails,
                repoRehash = repoRehash
            )
        } finally {
            git.repository?.close()
            git.close()
        }
    }

    /**
     * Escaneia o repositório para extrair idades de linhas de código.
     * Carrega dados armazenados para atualizações incrementais.
     */
    private fun updateFromObservable(
        repo: Repository,
        revWalk: RevWalk,
        head: RevCommit,
        git: Git,
        adapter: JGitRepositoryAdapter,
        dataPath: java.nio.file.Path,
        colleagues: Colleagues,
        emails: HashSet<String>,
        repoRehash: String
    ): List<Fact> {
        var storedHead: RevCommit? = null
        var ageData = CodeLineAges()

        // Carrega dados existentes se houver.
        try {
            val file = dataPath.toFile()
            val iStream = ObjectInputStream(FileInputStream(file))
            val storedHeadId = iStream.readUTF()
            Logger.debug { "Stored repo head: $storedHeadId" }
            storedHead = revWalk.parseCommit(repo.resolve(storedHeadId))
            if (storedHead == head) {
                return emptyList()  // TODO: Send saved stats in such case.
            }
            ageData = (iStream.readObject() ?: CodeLineAges()) as CodeLineAges
        } catch (e: FileNotFoundException) {
            // Primeira execução, sem dados armazenados.
        } catch (e: Exception) {
            Logger.error(e, "Failed to read longevity data. " +
                "CAUTION: data will be recomputed.")
        }

        // Atualiza idades processando linhas.
        val linesObservable = getLinesObservable(
            repo = repo,
            head = head,
            tail = storedHead,
            git = git,
            adapter = adapter
        )

        linesObservable.blockingSubscribe({ line ->
            Logger.trace { "Scanning: $line" }
            if (line.isDeleted) {
                if (ageData.lastingLines.contains(line.oldId)) {
                    line.age += ageData.lastingLines.remove(line.oldId)!!.age
                }
                val aggrAge = ageData.aggrAges.getOrPut(line.authorEmail) {
                    CodeLineAges.AggrAge()
                }
                aggrAge.sum += line.age
                aggrAge.count += 1

                colleagues.collect(line)
            } else {
                var age = line.age
                if (ageData.lastingLines.contains(line.oldId)) {
                    age += ageData.lastingLines.remove(line.oldId)!!.age
                }
                ageData.lastingLines[line.newId] = CodeLineAges.LineInfo(
                    age, line.authorEmail
                )
            }
        }, { error ->
            Logger.error(error as Exception, "Error processing longevity lines")
        })

        // Persiste dados para execuções subsequentes.
        try {
            val file = dataPath.toFile()
            val oStream = ObjectOutputStream(FileOutputStream(file))
            oStream.writeUTF(head.name)
            oStream.writeObject(ageData)
        } catch (e: Exception) {
            Logger.error(e, "Failed to save longevity data. " +
                "CAUTION: data will be recomputed on a next run.")
        }

        return calculateFacts(ageData, emails, repoRehash, colleagues)
    }

    /**
     * Calcula e retorna fatos de longevidade a partir dos dados acumulados.
     */
    private fun calculateFacts(
        ages: CodeLineAges,
        emails: HashSet<String>,
        repoRehash: String,
        colleagues: Colleagues
    ): List<Fact> {
        var repoTotal = 0
        var repoSum: Long = 0
        val aggrAges: HashMap<String, CodeLineAges.AggrAge> = hashMapOf()

        ages.aggrAges.forEach { (email, aggrAge) ->
            repoSum += aggrAge.sum
            repoTotal += aggrAge.count
            if (emails.contains(email)) {
                aggrAges[email] = aggrAge
            }
        }

        ages.lastingLines.forEach { (_, info) ->
            val aggrAge = aggrAges.getOrPut(info.email) { CodeLineAges.AggrAge() }
            aggrAge.sum += info.age
            aggrAge.count += 1

            repoSum += info.age
            repoTotal += 1
        }

        val secondsInDay = 86400
        val repoAvg = if (repoTotal > 0) repoSum / repoTotal else 0
        val facts = mutableListOf<Fact>()

        // LINE_LONGEVITY_REPO: média geral do repositório.
        facts.add(
            Fact(
                repoRehash = repoRehash,
                code = FactCodes.LINE_LONGEVITY_REPO,
                key = 0,
                value = repoAvg.toString(),
                authorEmail = Email("repo@placeholder")
            )
        )

        val repoAvgDays = repoAvg / secondsInDay
        Logger.info { "Repo average code line age is $repoAvgDays days, " +
            "lines total: $repoTotal" }

        // LINE_LONGEVITY: média por autor.
        for (email in emails) {
            val aggrAge = aggrAges[email] ?: CodeLineAges.AggrAge()
            val avg = if (aggrAge.count > 0) aggrAge.sum / aggrAge.count else 0
            facts.add(
                Fact(
                    repoRehash = repoRehash,
                    code = FactCodes.LINE_LONGEVITY,
                    key = 0,
                    value = avg.toString(),
                    authorEmail = Email(email)
                )
            )
        }

        // COLLEAGUES: fatos de colegas.
        facts.addAll(colleagues.calculateFacts())

        Logger.info { "Calculated ${facts.size} longevity facts" }
        return facts
    }

    /**
     * Retorna um Observable de linhas de código (vivas e deletadas)
     * entre as revisões do repositório.
     *
     * Algoritmo:
     * 1. Constrói mapa de arquivos HEAD com suas linhas
     * 2. Percorre diffs para trás na história:
     *    - Inserções: emite CodeLine(from=commit_line, to=head_line), remove do mapa
     *    - Deleções: adiciona linhas placeholder no mapa
     *    - Renomeações: move no mapa de arquivos
     * 3. No commit tail: emite todas as linhas restantes como CodeLine
     */
    private fun getLinesObservable(
        repo: Repository,
        head: RevCommit,
        tail: RevCommit?,
        git: Git,
        adapter: JGitRepositoryAdapter
    ): Observable<CodeLine> = Observable.create { subscriber ->

        val headWalk = TreeWalk(repo)
        headWalk.isRecursive = true
        headWalk.addTree(head.tree)

        val files: MutableMap<String, ArrayList<RevCommitLine>> = mutableMapOf()

        // Constrói mapa de nomes de arquivo e suas linhas no HEAD.
        while (headWalk.next()) {
            try {
                val path = headWalk.pathString
                val fileId = headWalk.getObjectId(0)
                val fileLoader = repo.open(fileId)
                if (!RawText.isBinary(fileLoader.openStream())) {
                    val fileText = RawText(fileLoader.bytes)
                    val lines = ArrayList<RevCommitLine>(fileText.size())
                    for (idx in 0 until fileText.size()) {
                        lines.add(RevCommitLine(head, fileId, path, idx, false))
                    }
                    files[path] = lines
                }
            } catch (e: Exception) {
                // Ignora arquivos que não podem ser lidos.
            }
        }

        // Obtém observable de diffs do JGit.
        val diffObservable = getJGitObservable(git, adapter)

        diffObservable
            .takeWhile { (commit, _) -> commit != tail }
            .subscribe({ (commit, diffs) ->
                // Passo para trás na história de commits.
                // Percorre diffs de trás para frente para lidar com renomeações duplas.
                for ((diff, editList) in diffs!!.asReversed()) {
                    val oldPath = diff.oldPath
                    val oldId = diff.oldId.toObjectId()
                    val newPath = diff.newPath
                    val newId = diff.newId.toObjectId()
                    Logger.trace { "old: '$oldPath', new: '$newPath'" }

                    // Arquivo foi deletado: inicializa array de linhas no mapa.
                    if (diff.changeType == DiffEntry.ChangeType.DELETE) {
                        val fileLoader = repo.open(oldId)
                        val fileText = RawText(fileLoader.bytes)
                        files[oldPath] = ArrayList(fileText.size())
                    }

                    // Se deletado, newPath é /dev/null.
                    val path = if (newPath != DiffEntry.DEV_NULL) {
                        newPath
                    } else {
                        oldPath
                    }
                    val lines = files[path] ?: continue

                    // Atualiza array de linhas com inserções (de trás para frente).
                    for (edit in editList.asReversed()) {
                        val insCount = edit.lengthB
                        if (insCount > 0) {
                            val insStart = edit.beginB
                            val insEnd = edit.endB
                            Logger.trace { "ins ($insStart, $insEnd)" }

                            for (idx in insStart until insEnd) {
                                val from = RevCommitLine(
                                    commit!!, newId, newPath, idx, false
                                )
                                try {
                                    val to = lines[idx]
                                    val cl = CodeLine(repo, from, to)
                                    Logger.trace { "Collected: $cl" }
                                    subscriber.onNext(cl)
                                } catch (e: IndexOutOfBoundsException) {
                                    Logger.error(e, "No line at $idx; commit: " +
                                        "${commit.name}; '${commit.shortMessage}'")
                                    throw e
                                }
                            }
                            lines.subList(insStart, insEnd).clear()
                        }
                    }

                    // Atualiza array de linhas com deleções.
                    for (edit in editList) {
                        val delCount = edit.lengthA
                        if (delCount > 0) {
                            val delStart = edit.beginA
                            val delEnd = edit.endA
                            Logger.trace { "del ($delStart, $delEnd)" }

                            val tmpLines = ArrayList<RevCommitLine>(delCount)
                            for (idx in delStart until delEnd) {
                                tmpLines.add(
                                    RevCommitLine(commit!!, oldId, oldPath, idx, true)
                                )
                            }
                            lines.addAll(delStart, tmpLines)
                        }
                    }

                    // Arquivo renomeado: ajusta o mapa.
                    if (diff.changeType == DiffEntry.ChangeType.RENAME) {
                        files[oldPath] = files.remove(newPath)!!
                    }
                }
            }, { error ->
                subscriber.onError(error)
            }, {
                // Se tail foi dado, o mapa contém linhas não reivindicadas
                // (adicionadas antes do tail). Emite todas.
                if (tail != null) {
                    val tailWalk = TreeWalk(repo)
                    tailWalk.isRecursive = true
                    tailWalk.addTree(tail.tree)

                    while (tailWalk.next()) {
                        val filePath = tailWalk.pathString
                        val lines = files[filePath]
                        if (lines != null) {
                            val fileId = tailWalk.getObjectId(0)
                            for (idx in 0 until lines.size) {
                                val from = RevCommitLine(
                                    tail, fileId, filePath, idx, false
                                )
                                val cl = CodeLine(repo, from, lines[idx])
                                Logger.trace { "Collected (tail): $cl" }
                                subscriber.onNext(cl)
                            }
                        }
                    }
                }
                subscriber.onComplete()
            })
    }

    /**
     * Cria um Observable de diffs JGit para percorrer a história de commits.
     */
    private fun getJGitObservable(
        git: Git,
        adapter: JGitRepositoryAdapter
    ): Observable<JgitData> = Observable.create { subscriber ->
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

            try {
                val diffEntries = df.scan(parentCommit, commit)
                    .filter { diff -> diff.changeType != DiffEntry.ChangeType.COPY }
                    .filter { diff ->
                        val path = if (diff.newPath != DiffEntry.DEV_NULL) {
                            diff.newPath
                        } else {
                            diff.oldPath
                        }
                        val fileId = if (diff.newPath != DiffEntry.DEV_NULL) {
                            diff.newId.toObjectId()
                        } else {
                            diff.oldId.toObjectId()
                        }
                        val stream = try {
                            repo.open(fileId).openStream()
                        } catch (e: Exception) { null }
                        stream != null && !RawText.isBinary(stream)
                    }

                val jgitDiffs = diffEntries.map { diff ->
                    JgitDiffEntry(diff, df.toFileHeader(diff).toEditList())
                }

                subscriber.onNext(JgitData(commit, jgitDiffs))
            } catch (e: Exception) {
                Logger.error(e, "Error scanning diffs for commit ${commit.name}")
            }

            commit = parentCommit
        }

        subscriber.onComplete()
    }
}

/**
 * Dados de diff JGit: commit e lista de diffs com edits.
 */
data class JgitData(
    val commit: RevCommit?,
    val diffs: List<JgitDiffEntry>?
)

/**
 * Entrada de diff JGit com DiffEntry e EditList.
 */
data class JgitDiffEntry(
    val diffEntry: DiffEntry,
    val editList: org.eclipse.jgit.diff.EditList
)
