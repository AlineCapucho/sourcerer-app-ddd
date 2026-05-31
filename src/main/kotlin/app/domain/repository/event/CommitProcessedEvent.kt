package app.domain.repository.event

import app.domain.repository.entity.Commit
import app.domain.shared.event.DomainEvent
import java.time.LocalDateTime

/**
 * Evento de domínio: Um commit foi processado (stats extraídas).
 * Representado por algo que aconteceu no passado.
 */
data class CommitProcessedEvent(
    val commit: Commit,
    val repoRehash: String,
    override val occurredOn: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventName: String = "commit.processed"
}
