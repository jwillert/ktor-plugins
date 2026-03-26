package dev.jwillert.ktor.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.plugin
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.principal
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OAuthSessionPlugin private constructor(val config: OAuthSessionConfig) {

    companion object Plugin : BaseApplicationPlugin<Application, OAuthSessionConfig, OAuthSessionPlugin> {

        override val key = AttributeKey<OAuthSessionPlugin>("OAuthSessionPlugin")

        override fun install(
            pipeline: Application,
            configure: OAuthSessionConfig.() -> Unit,
        ): OAuthSessionPlugin {
            val config = OAuthSessionConfig().apply(configure)
            val plugin = OAuthSessionPlugin(config)

            pipeline.install(Sessions) {
                cookie<OAuthSessionData>(config.sessionCookieName, storage = config.storage) {
                    serializer = JsonSessionSerializer(OAuthSessionData.serializer())
                }
            }

            config.logoutPath?.let { logoutPath ->
                pipeline.routing {
                    get(logoutPath) {
                        call.sessions.clear<OAuthSessionData>()
                        call.respondRedirect(config.redirectAfterLogout)
                    }
                }
            }

            return plugin
        }
    }
}

/**
 * Call this inside your OAuth callback route to exchange the token response for a server-side
 * session, optionally fetch user info, and redirect to [redirectTo].
 *
 * Example:
 * ```kotlin
 * authenticate("google") {
 *     get("/callback") {
 *         call.storeOAuthSession()
 *     }
 * }
 * ```
 */
suspend fun ApplicationCall.storeOAuthSession(
    userInfoUrl: String? = application.plugin(OAuthSessionPlugin).config.userInfoUrl,
    redirectTo: String = application.plugin(OAuthSessionPlugin).config.redirectAfterLogin,
    httpClient: HttpClient? = null,
) {
    val tokenResponse = principal<OAuthAccessTokenResponse.OAuth2>()
        ?: error("No OAuth2 token response in context. Ensure this is called inside an authenticated block.")

    val expiresAt = System.currentTimeMillis() + ((tokenResponse.expiresIn ?: 3600L) * 1000L)

    var userId: String? = null
    var email: String? = null
    var displayName: String? = null
    var extraClaims = emptyMap<String, String>()

    if (userInfoUrl != null) {
        val client = httpClient ?: HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            val infoResponse = client.get(userInfoUrl) {
                bearerAuth(tokenResponse.accessToken)
            }
            if (infoResponse.status.isSuccess()) {
                val obj = Json.parseToJsonElement(infoResponse.bodyAsText()).jsonObject
                userId = obj["sub"]?.jsonPrimitive?.contentOrNull
                email = obj["email"]?.jsonPrimitive?.contentOrNull
                displayName = obj["name"]?.jsonPrimitive?.contentOrNull
                extraClaims = obj.entries
                    .filter { it.key !in setOf("sub", "email", "name") }
                    .mapNotNull { (k, v) -> v.jsonPrimitive.contentOrNull?.let { k to it } }
                    .toMap()
            }
        } catch (_: Exception) {
            // user info is best-effort; continue without it
        } finally {
            if (httpClient == null) client.close()
        }
    }

    sessions.set(
        OAuthSessionData(
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            expiresAt = expiresAt,
            tokenType = tokenResponse.tokenType ?: "Bearer",
            userId = userId,
            email = email,
            displayName = displayName,
            extraClaims = extraClaims,
        )
    )

    respondRedirect(redirectTo)
}
