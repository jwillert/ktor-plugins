package dev.jwillert.ktor.auth.refresh

import dev.jwillert.ktor.auth.OAuthSessionData
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class TokenRefreshService(
    private val config: TokenRefreshConfig,
    engine: HttpClientEngine = CIO.create(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun refresh(session: OAuthSessionData): OAuthSessionData {
        val refreshToken = session.refreshToken
            ?: throw IllegalStateException("Cannot refresh: session has no refresh token")

        val response = httpClient.submitForm(
            url = config.accessTokenUrl,
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
            }
        )

        if (!response.status.isSuccess()) {
            throw RefreshTokenExpiredException(
                "Token refresh failed with status ${response.status.value}: ${response.bodyAsText()}"
            )
        }

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val newAccessToken = body["access_token"]?.jsonPrimitive?.content
            ?: throw RefreshTokenExpiredException("Token response missing access_token")
        val newRefreshToken = body["refresh_token"]?.jsonPrimitive?.content ?: refreshToken
        val expiresIn = body["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        val tokenType = body["token_type"]?.jsonPrimitive?.content ?: session.tokenType

        return session.copy(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresAt = System.currentTimeMillis() + (expiresIn * 1000),
            tokenType = tokenType,
        )
    }
}
