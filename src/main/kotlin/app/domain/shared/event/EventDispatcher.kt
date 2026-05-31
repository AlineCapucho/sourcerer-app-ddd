package app.domain.shared.event

/**
 * Despachante de eventos: Armazena e executa os handlers.
 * Responsável por registrar handlers e despachar eventos para eles.
 */
interface EventDispatcher {
    fun <T : DomainEvent> register(eventClass: Class<T>, handler: EventHandler<T>)
    fun <T : DomainEvent> dispatch(event: T)
    fun clear()
}
