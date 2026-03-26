package dev.jwillert.ktor.auth

import io.ktor.server.sessions.SessionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JsonSessionSerializer<T>(
    private val serializer: KSerializer<T>,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionSerializer<T> {
    override fun serialize(session: T): String = json.encodeToString(serializer, session)
    override fun deserialize(text: String): T = json.decodeFromString(serializer, text)
}
