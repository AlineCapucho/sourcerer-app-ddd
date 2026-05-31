package test.tests.infrastructure

import app.domain.repository.event.RepoHashingCompletedEvent
import app.domain.shared.event.DomainEvent
import app.domain.shared.event.EventHandler
import app.infrastructure.event.InMemoryEventDispatcher
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventDispatcherTest : Spek({
    given("InMemoryEventDispatcher") {
        it("dispatches events to registered handlers") {
            val dispatcher = InMemoryEventDispatcher()
            val receivedEvents = mutableListOf<DomainEvent>()

            dispatcher.register(RepoHashingCompletedEvent::class.java,
                object : EventHandler<RepoHashingCompletedEvent> {
                    override fun handle(event: RepoHashingCompletedEvent) {
                        receivedEvents.add(event)
                    }
                })

            val event = RepoHashingCompletedEvent("rehash", true)
            dispatcher.dispatch(event)

            assertEquals(1, receivedEvents.size)
            assertTrue(receivedEvents[0] is RepoHashingCompletedEvent)
        }

        it("supports multiple handlers for same event") {
            val dispatcher = InMemoryEventDispatcher()
            var handler1Called = false
            var handler2Called = false

            dispatcher.register(RepoHashingCompletedEvent::class.java,
                object : EventHandler<RepoHashingCompletedEvent> {
                    override fun handle(event: RepoHashingCompletedEvent) {
                        handler1Called = true
                    }
                })
            dispatcher.register(RepoHashingCompletedEvent::class.java,
                object : EventHandler<RepoHashingCompletedEvent> {
                    override fun handle(event: RepoHashingCompletedEvent) {
                        handler2Called = true
                    }
                })

            dispatcher.dispatch(RepoHashingCompletedEvent("rehash", true))

            assertTrue(handler1Called)
            assertTrue(handler2Called)
        }

        it("clear removes all handlers") {
            val dispatcher = InMemoryEventDispatcher()
            var called = false

            dispatcher.register(RepoHashingCompletedEvent::class.java,
                object : EventHandler<RepoHashingCompletedEvent> {
                    override fun handle(event: RepoHashingCompletedEvent) {
                        called = true
                    }
                })

            dispatcher.clear()
            dispatcher.dispatch(RepoHashingCompletedEvent("rehash", true))

            assertEquals(false, called)
        }
    }
})
