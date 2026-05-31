package app.domain.repository.event

import app.domain.repository.entity.Author
import app.domain.shared.event.DomainEvent
import java.time.LocalDateTime

/**
 * Evento de domínio: Autores foram descobertos em um repositório.
 * Representado por algo que aconteceu no passado.
 */
data class AuthorsDiscoveredEvent(
    val repoRehash: String,
    val authors: Set<Author>,
    override val occurredOn: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventName: String = "authors.discovered"
}
