package dev.jwillert.ktor.auth.storage

import io.ktor.server.sessions.SessionStorage
import io.lettuce.core.RedisClient
import io.lettuce.core.SetArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class RedisSessionStorage(private val redisClient: RedisClient) : SessionStorage {

    private val connection = redisClient.connect()
    private val sync = connection.sync()

    override suspend fun read(id: String): String = withContext(Dispatchers.IO) {
        sync.get(id) ?: throw NoSuchElementException("Session '$id' not found")
    }

    override suspend fun write(id: String, value: String): Unit = withContext(Dispatchers.IO) {
        val expiresAt = extractExpiresAt(value)
        val ttlSeconds = max(1L, (expiresAt - System.currentTimeMillis()) / 1000)
        sync.set(id, value, SetArgs.Builder.ex(ttlSeconds))
    }

    override suspend fun invalidate(id: String): Unit = withContext(Dispatchers.IO) {
        sync.del(id)
    }
}
