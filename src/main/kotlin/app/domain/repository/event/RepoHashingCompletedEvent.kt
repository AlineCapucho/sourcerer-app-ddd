package app.domain.repository.event

import app.domain.shared.event.DomainEvent
import java.time.LocalDateTime

/**
 * Evento de domínio: O hashing de um repositório foi concluído.
 * Representado por algo que aconteceu no passado.
 * Utilizado para notificar outros bounded contexts de uma mudança de estado.
 */
data class RepoHashingCompletedEvent(
    val repoRehash: String,
    val success: Boolean,
    val errorCode: Int = 0,
    override val occurredOn: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventName: String = "repo.hashing.completed"
}
