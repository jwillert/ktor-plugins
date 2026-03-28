package dev.jwillert.ddd.events

import kotlin.reflect.KClass

typealias EventHandler<T> = suspend (T) -> Unit

fun interface EventBusSender {
    suspend fun publish(events: List<Any>)
}

class EventBus : EventBusSender {
    private val handlers = mutableMapOf<KClass<*>, MutableList<EventHandler<*>>>()

    fun <T : Any> subscribe(type: KClass<T>, handler: EventHandler<T>) {
        handlers.getOrPut(type) { mutableListOf() }.add(handler)
    }

    inline fun <reified T : Any> subscribe(noinline handler: EventHandler<T>) {
        subscribe(T::class, handler)
    }

    override suspend fun publish(events: List<Any>) {
        events.forEach { event ->
            handlers[event::class]?.forEach { handler ->
                @Suppress("UNCHECKED_CAST")
                (handler as EventHandler<Any>)(event)
            }
        }
    }
}
