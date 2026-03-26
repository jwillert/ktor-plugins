package dev.jwillert.ktor.auth

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OAuthSessionData(
    val sessionId: String = UUID.randomUUID().toString(),
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: Long,
    val tokenType: String = "Bearer",
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val extraClaims: Map<String, String> = emptyMap(),
) {
    fun isExpired(): Boolean = System.currentTimeMillis() >= expiresAt

    fun isExpiringSoon(thresholdMs: Long): Boolean =
        System.currentTimeMillis() >= (expiresAt - thresholdMs)
}
