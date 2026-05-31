package app.domain.repository.event

import app.domain.shared.event.DomainEvent
import java.time.LocalDateTime

/**
 * Evento de domínio: Fatos de um repositório foram calculados.
 * Representado por algo que aconteceu no passado.
 */
data class FactsCalculatedEvent(
    val repoRehash: String,
    val factCount: Int,
    override val occurredOn: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventName: String = "facts.calculated"
}
