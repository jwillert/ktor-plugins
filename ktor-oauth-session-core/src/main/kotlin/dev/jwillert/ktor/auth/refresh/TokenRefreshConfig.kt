package dev.jwillert.ktor.auth.refresh

data class TokenRefreshConfig(
    val clientId: String,
    val clientSecret: String,
    val accessTokenUrl: String,
)
