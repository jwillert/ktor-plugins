# Changelog

All notable changes are documented here, grouped by module. Versions are unified across all modules.

## [Unreleased]

### ktor-ddd
- Initial release: `Entity`, `AggregateRoot` + `AggregateRootDelegate`, `DomainEvent`, `UnitOfWork`
- `DomainException`, `ValidationException`, `NotFoundException`
- `EventBus`, `DomainEvents` Ktor plugin with Koin-aware DSL

### ktor-ddd-exposed
- Initial release: `ExposedUnitOfWork` backed by Exposed `suspendTransaction`

## [0.3.0] - 2026-06-15

### ktor-vrt
- Initial release: kotlinx-html visual regression testing toolkit — `VrtHarness` (render → screenshot → diff), `Scenario`, `PlaywrightServerContainer` (pinned, deterministic), and the `VisualRegressionSpec` Kotest base
- Theme attributes and capture-wrapper classes are configurable (no framework lock-in)

### ktor-vrt-gradle-plugin
- Initial release: `id("dev.jwillert.ktor-vrt")` — creates the `vrt` source set and `vrtTest`/`vrtTestDocker` tasks, configured via the `ktorVrt { }` extension; pulls in `ktor-vrt` (and its test deps) transitively

---

## [0.1.0] - 2024-12-01

### ktor-oauth-session-core
- Initial release: server-side OAuth 2.0 session management for Ktor
- `OAuthSessionPlugin` with configurable storage, logout path, and post-login redirect
- `oauthSession()` auth provider with silent token refresh
- `InMemorySessionStorage`

### ktor-oauth-session-exposed
- Initial release: `ExposedSessionStorage` — SQL-backed session persistence via Exposed ORM

### ktor-oauth-session-redis
- Initial release: `RedisSessionStorage` — Redis-backed session persistence via Lettuce
