package dev.jwillert.ktor.auth.refresh

class RefreshTokenExpiredException(message: String = "Refresh token is expired or invalid") :
    Exception(message)
