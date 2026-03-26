package dev.jwillert.ktor.auth

import io.ktor.server.auth.Principal

data class OAuthSessionPrincipal(
    val sessionId: String,
    val accessToken: String,
    val userId: String?,
    val email: String?,
    val displayName: String?,
    val extraClaims: Map<String, String>,
) : Principal {
    constructor(session: OAuthSessionData) : this(
        sessionId = session.sessionId,
        accessToken = session.accessToken,
        userId = session.userId,
        email = session.email,
        displayName = session.displayName,
        extraClaims = session.extraClaims,
    )
}
