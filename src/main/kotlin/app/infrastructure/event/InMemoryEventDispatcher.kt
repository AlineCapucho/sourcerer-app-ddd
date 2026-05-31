package app.infrastructure.event

import app.domain.shared.event.DomainEvent
import app.domain.shared.event.EventDispatcher
import app.domain.shared.event.EventHandler

/**
 * Implementação em memória do despachante de eventos.
 * Armazena e executa os handlers de forma síncrona.
 */
class InMemoryEventDispatcher : EventDispatcher {

    private val handlers = mutableMapOf<Class<*>, MutableList<EventHandler<*>>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : DomainEvent> register(eventClass: Class<T>, handler: EventHandler<T>) {
        val eventHandlers = handlers.getOrPut(eventClass) { mutableListOf() }
        eventHandlers.add(handler)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : DomainEvent> dispatch(event: T) {
        val eventHandlers = handlers[event.javaClass] ?: return
        eventHandlers.forEach { handler ->
            (handler as EventHandler<T>).handle(event)
        }
    }

    override fun clear() {
        handlers.clear()
    }
}
