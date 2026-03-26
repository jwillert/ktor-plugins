package dev.jwillert.ktor.auth.storage

private val EXPIRES_AT_REGEX = """"expiresAt"\s*:\s*(\d+)""".toRegex()

/**
 * Extracts the `expiresAt` epoch-millis value from a JSON-serialized [OAuthSessionData] string.
 * Falls back to 1 hour from now if the field is not found.
 */
fun extractExpiresAt(serializedValue: String): Long =
    EXPIRES_AT_REGEX.find(serializedValue)
        ?.groupValues?.get(1)?.toLong()
        ?: (System.currentTimeMillis() + 3_600_000)
