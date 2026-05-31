package app.domain.repository.port

import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.entity.Repo
import app.domain.repository.valueobject.AuthorDistance
import app.domain.repository.valueobject.Fact
import app.domain.repository.valueobject.ProcessEntry
import app.domain.user.entity.User
import app.domain.user.valueobject.UserEmail

/**
 * Port (interface) para comunicação com o servidor API.
 * ACL: Isola o domínio de detalhes de protocolo (HTTP, Protobuf).
 * A infraestrutura implementa esta interface.
 */
interface ServerApiPort {
    /**
     * Autentica o usuário no servidor.
     */
    fun authorize(username: String, password: String): ApiResult<Unit>

    /**
     * Obtém dados do usuário do servidor.
     */
    fun getUser(): ApiResult<UserData>

    /**
     * Envia dados do usuário ao servidor.
     */
    fun postUser(emails: List<UserEmail>, repos: List<Repo>): ApiResult<Unit>

    /**
     * Envia repositório ao servidor e recebe versão com histórico.
     */
    fun postRepo(repo: Repo): ApiResult<RepoData>

    /**
     * Envia commits processados ao servidor.
     */
    fun postCommits(commits: List<Commit>): ApiResult<Unit>

    /**
     * Remove commits do servidor.
     */
    fun deleteCommits(commits: List<Commit>): ApiResult<Unit>

    /**
     * Envia fatos calculados ao servidor.
     */
    fun postFacts(facts: List<Fact>): ApiResult<Unit>

    /**
     * Envia autores ao servidor.
     */
    fun postAuthors(authors: List<Author>): ApiResult<Unit>

    /**
     * Cria uma sessão de processamento no servidor.
     */
    fun postProcessCreate(requestNumEntries: Int): ApiResult<ProcessData>

    /**
     * Atualiza o status de processamento no servidor.
     */
    fun postProcess(entries: List<ProcessEntry>): ApiResult<Unit>

    /**
     * Envia distâncias entre autores ao servidor.
     */
    fun postAuthorDistances(distances: List<AuthorDistance>): ApiResult<Unit>
}

/**
 * Resultado genérico de operações da API.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int = 0, val message: String = "") : ApiResult<Nothing>()

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw ApiException(code, message)
    }

    fun onErrorThrow() {
        if (this is Error) throw ApiException(code, message)
    }
}

class ApiException(val code: Int, override val message: String) : RuntimeException(message)

/**
 * Dados do usuário retornados pelo servidor.
 */
data class UserData(
    val emails: List<UserEmail>,
    val repos: List<RepoData>
)

/**
 * Dados do repositório retornados pelo servidor.
 */
data class RepoData(
    val rehash: String = "",
    val initialCommitRehash: String = "",
    val emails: List<String> = listOf(),
    val commits: List<CommitData> = listOf(),
    val processEntryId: Int = 0
)

/**
 * Dados de commit retornados pelo servidor.
 */
data class CommitData(
    val rehash: String,
    val repoRehash: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val dateTimestamp: Long = 0,
    val treeRehash: String = ""
)

/**
 * Dados de processo retornados pelo servidor.
 */
data class ProcessData(
    val id: Int = 0,
    val entries: List<ProcessEntry> = listOf()
)
