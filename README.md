# ktor-oauth-session

[![CI](https://github.com/willert-harbor/ktor-plugins/actions/workflows/ci.yml/badge.svg)](https://github.com/willert-harbor/ktor-plugins/actions/workflows/ci.yml)
[![Maven](https://img.shields.io/badge/maven-dev.jwillert-blue)](https://github.com/willert-harbor/ktor-plugins/packages)
![Kotlin](https://img.shields.io/badge/kotlin-2.1.20-7F52FF)
![Ktor](https://img.shields.io/badge/ktor-3.0.3-087CFA)

Server-side OAuth 2.0 session management for [Ktor](https://ktor.io). Stores tokens and user data **on the server** — only a session ID lives in the browser cookie. Supports automatic token refresh and pluggable storage backends.

## Why

Ktor's built-in `auth-oauth` plugin handles the OAuth login flow, and `sessions` handles cookies — but by default the entire token payload is stored inside the cookie. This plugin bridges the gap:

- Tokens never leave the server
- Sessions survive server restarts (with Exposed or Redis)
- Automatic silent token refresh before expiry
- Works alongside Ktor's standard `oauth()` provider — no reimplementation of the login flow

## Modules

| Artifact | Description |
|---|---|
| `dev.jwillert:ktor-oauth-session-core` | Core plugin, in-memory storage, token refresh |
| `dev.jwillert:ktor-oauth-session-exposed` | SQL storage via [Exposed ORM](https://github.com/JetBrains/Exposed) |
| `dev.jwillert:ktor-oauth-session-redis` | Redis storage via [Lettuce](https://lettuce.io) |

## Installation

```kotlin
// settings.gradle.kts
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/jwillert/ktor-plugins")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.jwillert:ktor-oauth-session-core:0.1.0")

    // pick one or both:
    implementation("dev.jwillert:ktor-oauth-session-exposed:0.1.0")
    implementation("dev.jwillert:ktor-oauth-session-redis:0.1.0")
}
```

## Quick Start

### 1. Choose a storage backend

```kotlin
// In-memory (no persistence — good for development)
val storage = InMemorySessionStorage()

// SQL via Exposed
val database = Database.connect(dataSource)
val storage = ExposedSessionStorage(database)

// Redis via Lettuce
val storage = RedisSessionStorage(RedisClient.create("redis://localhost:6379"))
```

### 2. Install the plugin

```kotlin
install(OAuthSessionPlugin) {
    this.storage = storage
    logoutPath = "/logout"            // auto-registers a logout route
    redirectAfterLogin = "/profile"
    redirectAfterLogout = "/"
    userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo"  // optional
}
```

### 3. Configure authentication

```kotlin
install(Authentication) {
    // Ktor's built-in OAuth provider — handles /login redirect + /callback
    oauth("google") {
        urlProvider = { "http://localhost:8080/callback" }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = "google",
                authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                accessTokenUrl = "https://oauth2.googleapis.com/token",
                clientId = System.getenv("GOOGLE_CLIENT_ID")!!,
                clientSecret = System.getenv("GOOGLE_CLIENT_SECRET")!!,
                defaultScopes = listOf("openid", "email", "profile"),
            )
        }
        client = HttpClient(CIO)
    }

    // This plugin's session provider — validates sessions on protected routes
    oauthSession("authenticated") {
        loginPath = "/login"
        refreshThresholdMs = 60_000   // refresh if expiry < 60 s away
        tokenRefreshService = TokenRefreshService(
            TokenRefreshConfig(
                clientId = System.getenv("GOOGLE_CLIENT_ID")!!,
                clientSecret = System.getenv("GOOGLE_CLIENT_SECRET")!!,
                accessTokenUrl = "https://oauth2.googleapis.com/token",
            )
        )
    }
}
```

### 4. Define routes

```kotlin
routing {
    get("/") { call.respondText("Hello! <a href='/login'>Login</a>") }

    authenticate("google") {
        get("/login") { /* Ktor redirects to Google automatically */ }
        get("/callback") {
            // Exchanges the code for tokens, fetches user info, stores session
            call.storeOAuthSession()
        }
    }

    authenticate("authenticated") {
        get("/profile") {
            val user = call.principal<OAuthSessionPrincipal>()!!
            call.respondText("Hello, ${user.displayName ?: user.email}!")
        }
    }
}
```

### 5. Run

```
GOOGLE_CLIENT_ID=xxx GOOGLE_CLIENT_SECRET=yyy ./gradlew :sample:run
```

---

## Configuration Reference

### `OAuthSessionPlugin`

| Property | Type | Default | Description |
|---|---|---|---|
| `storage` | `SessionStorage` | `InMemorySessionStorage()` | Where sessions are stored |
| `sessionCookieName` | `String` | `"oauth_session"` | Name of the session cookie |
| `logoutPath` | `String?` | `"/logout"` | Path for the auto-registered logout route. Set to `null` to disable |
| `redirectAfterLogin` | `String` | `"/"` | Redirect target after `storeOAuthSession()` |
| `redirectAfterLogout` | `String` | `"/"` | Redirect target after logout |
| `userInfoUrl` | `String?` | `null` | Provider userinfo endpoint. When set, `storeOAuthSession()` fetches `userId`, `email`, `displayName` |

### `oauthSession()`

| Property | Type | Default | Description |
|---|---|---|---|
| `loginPath` | `String` | `"/login"` | Redirect target when session is missing or expired |
| `refreshThresholdMs` | `Long` | `60_000` | Refresh token if session expires within this window (ms) |
| `tokenRefreshService` | `TokenRefreshService?` | `null` | Handles silent token refresh. When `null`, expired sessions redirect to login |

### `storeOAuthSession()`

```kotlin
suspend fun ApplicationCall.storeOAuthSession(
    userInfoUrl: String? = /* from plugin config */,
    redirectTo: String = /* from plugin config */,
    httpClient: HttpClient? = null,  // reuse an existing client
)
```

Called inside the OAuth callback route. Stores the session and redirects.

---

## Storage Backends

### ExposedSessionStorage

Persists sessions to any JDBC database using [Exposed ORM](https://github.com/JetBrains/Exposed). Requires a `Database` instance — bring your own connection pool (HikariCP recommended).

```kotlin
val dataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
    username = "user"
    password = "password"
})
val storage = ExposedSessionStorage(Database.connect(dataSource))
```

- Table `oauth_sessions` is created automatically on first use
- Expired sessions are removed on read
- Call `storage.purgeExpired()` from a scheduled job to clean up proactively

### RedisSessionStorage

Persists sessions to Redis using [Lettuce](https://lettuce.io). Sessions expire automatically via Redis TTL — no cleanup needed.

```kotlin
val storage = RedisSessionStorage(RedisClient.create("redis://localhost:6379"))
```

### InMemorySessionStorage

Non-persistent, concurrent in-memory store. Suitable for development or stateless deployments behind a sticky load balancer.

```kotlin
val storage = InMemorySessionStorage()
```

---

## Session Data

`OAuthSessionData` is what gets stored server-side:

```kotlin
@Serializable
data class OAuthSessionData(
    val sessionId: String,        // UUID, used as the session cookie value
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long,          // epoch millis
    val tokenType: String,        // "Bearer"
    val userId: String?,          // from userinfo endpoint
    val email: String?,
    val displayName: String?,
    val extraClaims: Map<String, String>,
)
```

`OAuthSessionPrincipal` is what your route handlers receive via `call.principal<OAuthSessionPrincipal>()`:

```kotlin
data class OAuthSessionPrincipal(
    val sessionId: String,
    val accessToken: String,
    val userId: String?,
    val email: String?,
    val displayName: String?,
    val extraClaims: Map<String, String>,
)
```

---

## Token Refresh

When `tokenRefreshService` is configured and a session is expiring within `refreshThresholdMs`, the plugin silently refreshes the token before the request reaches your handler — completely transparent to the client.

```kotlin
val refreshService = TokenRefreshService(
    TokenRefreshConfig(
        clientId = "...",
        clientSecret = "...",
        accessTokenUrl = "https://oauth2.googleapis.com/token",
    )
)
```

If the refresh token is also expired, the session is cleared and the user is redirected to `loginPath`.

---

## Requirements

- Kotlin 2.1+
- Ktor 3.0+
- JVM 17+

---

## License

[MIT](LICENSE)
