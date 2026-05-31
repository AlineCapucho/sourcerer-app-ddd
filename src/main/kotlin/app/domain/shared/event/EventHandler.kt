package app.domain.shared.event

/**
 * Handler: Executa o processamento quando o evento é chamado.
 */
interface EventHandler<T : DomainEvent> {
    fun handle(event: T)
}
