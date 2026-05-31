package app.domain.repository.aggregate

import app.domain.repository.entity.Author
import app.domain.repository.entity.Commit
import app.domain.repository.entity.Repo
import app.domain.repository.valueobject.RepoMeta
import app.domain.shared.event.DomainEvent

/**
 * Agregado de Repositório.
 * Raiz do agregado: Repo.
 * Conjunto de objetos associados com propósito de mudança de dados.
 * Fortemente acoplado internamente, fracamente acoplado externamente.
 */
class RepoAggregate(
    val repo: Repo
) {
    private val _authors: MutableSet<Author> = mutableSetOf()
    private val _events: MutableList<DomainEvent> = mutableListOf()

    val authors: Set<Author> get() = _authors.toSet()
    val events: List<DomainEvent> get() = _events.toList()

    /**
     * Registra um autor descoberto no repositório.
     * Se o autor já existe (mesmo email), atualiza o nome se mais completo.
     */
    fun registerAuthor(author: Author) {
        val existing = _authors.find { it.email == author.email }
        if (existing != null) {
            existing.updateNameIfMoreComplete(author.name)
        } else {
            author.associateToRepo(repo.rehash)
            _authors.add(author)
        }
    }

    /**
     * Registra múltiplos autores.
     */
    fun registerAuthors(authors: Collection<Author>) {
        authors.forEach { registerAuthor(it) }
    }

    /**
     * Encontra o primeiro rehash de commit que existe tanto localmente
     * quanto no servidor (overlap).
     */
    fun findFirstOverlappingCommitRehash(localRehashes: List<String>): String? {
        val serverHistoryRehashes = repo.commits
            .map { commit -> commit.rehash }
            .toHashSet()
        return localRehashes.firstOrNull { rehash ->
            serverHistoryRehashes.contains(rehash)
        }
    }

    /**
     * Retorna commits do servidor que não existem mais localmente
     * (anteriores ao primeiro overlap).
     */
    fun findDeletedCommits(localRehashes: List<String>): List<Commit> {
        val firstOverlap = findFirstOverlappingCommitRehash(localRehashes)
        return repo.commits.takeWhile { it.rehash != firstOverlap }
    }

    /**
     * Filtra emails conhecidos pelo servidor e pelo usuário.
     */
    fun filterKnownEmails(
        allEmails: Set<String>,
        userEmails: Set<String>
    ): Set<String> {
        val knownEmails = hashSetOf<String>()
        knownEmails.addAll(userEmails)
        knownEmails.addAll(repo.emails)
        return knownEmails.filter { allEmails.contains(it) }.toHashSet()
    }

    /**
     * Registra um evento de domínio.
     */
    fun addEvent(event: DomainEvent) {
        _events.add(event)
    }

    /**
     * Limpa os eventos após despacho.
     */
    fun clearEvents() {
        _events.clear()
    }
}
