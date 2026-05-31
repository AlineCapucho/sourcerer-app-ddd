package app.domain.shared.event

import java.time.LocalDateTime

/**
 * Base interface for all domain events.
 * Todo evento deve ser representado por algo que aconteceu no passado.
 */
interface DomainEvent {
    val occurredOn: LocalDateTime
    val eventName: String
}
