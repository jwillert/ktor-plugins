# Changelog

All notable changes are documented here, grouped by module. Versions are unified across all modules.

## [Unreleased]

### ktor-ddd
- Initial release: `Entity`, `AggregateRoot` + `AggregateRootDelegate`, `DomainEvent`, `UnitOfWork`
- `DomainException`, `ValidationException`, `NotFoundException`
- `EventBus`, `DomainEvents` Ktor plugin with Koin-aware DSL

### ktor-ddd-exposed
- Initial release: `ExposedUnitOfWork` backed by Exposed `suspendTransaction`

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
