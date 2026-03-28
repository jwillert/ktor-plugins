package dev.jwillert.ddd.events

import dev.jwillert.ddd.DomainEvent
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject

class EventHandlerContext<T : DomainEvent>(
    val event: T,
    val application: Application
) {
    inline fun <reified D : Any> inject(): Lazy<D> = application.inject()
    inline fun <reified D : Any> get(): D = application.get()

    suspend fun <R> withContext(block: suspend () -> R): R = block()
}

class EventBusDsl(
    @PublishedApi internal val eventBus: EventBus,
    @PublishedApi internal val application: Application
) {
    inline fun <reified T : DomainEvent> on(
        noinline handler: suspend EventHandlerContext<T>.() -> Unit
    ) {
        val wrappedHandler: EventHandler<T> = { event ->
            EventHandlerContext(event, application).handler()
        }
        eventBus.subscribe(T::class, wrappedHandler)
    }
}

operator fun EventBus.invoke(application: Application, block: EventBusDsl.() -> Unit) {
    EventBusDsl(this, application).block()
}

class DomainEventsConfig {
    internal val handlers = mutableListOf<EventBusDsl.() -> Unit>()

    fun handlers(block: EventBusDsl.() -> Unit) {
        handlers.add(block)
    }
}

val DomainEvents = createApplicationPlugin(
    name = "DomainEvents",
    createConfiguration = ::DomainEventsConfig
) {
    val eventBus by application.inject<EventBus>()

    pluginConfig.handlers.forEach { block ->
        EventBusDsl(eventBus, application).block()
    }
}
