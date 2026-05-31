package app.domain.repository.port

import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.valueobject.DiffFile

/**
 * Port (interface) para acesso ao repositório Git.
 * ACL: Isola o domínio de detalhes de implementação do JGit.
 * A infraestrutura implementa esta interface.
 */
interface GitRepositoryPort {

    /**
     * Abre um repositório Git no caminho especificado.
     */
    fun open(path: String)

    /**
     * Fecha o repositório Git.
     */
    fun close()

    /**
     * Busca todos os rehashes de commits e autores do repositório.
     * Retorna: (rehashes em ordem, autores, contagem de commits por email)
     */
    fun fetchRehashesAndAuthors(): Triple<List<String>, Set<Author>, Map<String, Int>>

    /**
     * Retorna um iterador/stream de commits com diffs para processamento.
     * @param filteredEmails se não nulo, filtra apenas commits desses emails
     */
    fun crawlCommits(
        repoRehash: String,
        totalCommitCount: Int = 0,
        filteredEmails: Set<String>? = null
    ): CommitStream

    /**
     * Retorna um iterador/stream de dados de caminho para cálculo de distância.
     */
    fun crawlPaths(): PathStream

    /**
     * Obtém a configuração do repositório (remote origin, user info).
     */
    fun getRemoteOrigin(): String

    /**
     * Obtém o nome do usuário configurado no git.
     */
    fun getUserName(): String

    /**
     * Obtém o email do usuário configurado no git.
     */
    fun getUserEmail(): String
}

/**
 * Interface para stream de commits (abstrai RxJava do domínio).
 */
interface CommitStream {
    /**
     * Processa cada commit com o handler fornecido.
     * @param onNext chamado para cada commit
     * @param onError chamado em caso de erro
     * @param onComplete chamado ao finalizar
     */
    fun subscribe(
        onNext: (Commit) -> Unit,
        onError: (Throwable) -> Unit,
        onComplete: () -> Unit
    )
}

/**
 * Interface para stream de dados de caminho (para distância entre autores).
 */
interface PathStream {
    /**
     * Processa cada entrada de caminho.
     */
    fun subscribe(
        onNext: (PathData) -> Unit,
        onError: (Throwable) -> Unit,
        onComplete: () -> Unit
    )
}

/**
 * Dados de caminho para cálculo de distância entre autores.
 */
data class PathData(
    val email: String,
    val paths: List<String>,
    val timestamp: Long
)
