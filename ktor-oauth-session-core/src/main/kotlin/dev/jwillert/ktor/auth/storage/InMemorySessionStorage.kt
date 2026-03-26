package dev.jwillert.ktor.auth.storage

import io.ktor.server.sessions.SessionStorage
import java.util.concurrent.ConcurrentHashMap

class InMemorySessionStorage : SessionStorage {

    private val store = ConcurrentHashMap<String, String>()

    override suspend fun read(id: String): String =
        store[id] ?: throw NoSuchElementException("Session '$id' not found")

    override suspend fun write(id: String, value: String) {
        store[id] = value
    }

    override suspend fun invalidate(id: String) {
        store.remove(id)
    }
}
