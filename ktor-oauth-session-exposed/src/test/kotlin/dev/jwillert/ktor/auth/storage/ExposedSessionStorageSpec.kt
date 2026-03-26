package dev.jwillert.ktor.auth.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.jwillert.ktor.auth.OAuthSessionData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

class ExposedSessionStorageSpec : DescribeSpec({

    // Use H2 in-memory for fast tests (no Testcontainers needed for unit tests)
    val datasource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:h2:mem:test_sessions_${System.nanoTime()};DB_CLOSE_DELAY=-1"
        driverClassName = "org.h2.Driver"
        maximumPoolSize = 5
    })
    val database = Database.connect(datasource)
    lateinit var storage: ExposedSessionStorage

    fun sessionJson(expiresAt: Long = System.currentTimeMillis() + 3_600_000): String =
        Json.encodeToString(
            OAuthSessionData(accessToken = "tok", refreshToken = "ref", expiresAt = expiresAt)
        )

    beforeEach { storage = ExposedSessionStorage(database) }

    afterSpec { datasource.close() }

    describe("write and read") {
        it("reads back the written value") {
            val json = sessionJson()
            storage.write("s1", json)
            storage.read("s1") shouldBe json
        }
        it("overwrites an existing session") {
            storage.write("s2", sessionJson())
            val updated = sessionJson(System.currentTimeMillis() + 7_200_000)
            storage.write("s2", updated)
            storage.read("s2") shouldBe updated
        }
    }

    describe("read when missing") {
        it("throws NoSuchElementException for unknown id") {
            shouldThrow<NoSuchElementException> { storage.read("missing") }
        }
    }

    describe("expired session") {
        it("throws NoSuchElementException and removes the row when session is expired") {
            val expired = sessionJson(expiresAt = System.currentTimeMillis() - 1000)
            storage.write("exp1", expired)
            shouldThrow<NoSuchElementException> { storage.read("exp1") }
        }
    }

    describe("invalidate") {
        it("removes the session") {
            storage.write("s3", sessionJson())
            storage.invalidate("s3")
            shouldThrow<NoSuchElementException> { storage.read("s3") }
        }
        it("does not throw when invalidating a non-existent id") {
            storage.invalidate("ghost")
        }
    }
})
