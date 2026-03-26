package dev.jwillert.ktor.auth.storage

import dev.jwillert.ktor.auth.OAuthSessionData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.lettuce.core.RedisClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class RedisSessionStorageSpec : DescribeSpec({

    val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)

    lateinit var redisClient: RedisClient
    lateinit var storage: RedisSessionStorage

    beforeSpec {
        redisContainer.start()
        redisClient = RedisClient.create("redis://${redisContainer.host}:${redisContainer.getMappedPort(6379)}")
    }

    afterSpec {
        redisClient.shutdown()
        redisContainer.stop()
    }

    beforeEach {
        storage = RedisSessionStorage(redisClient)
        // flush all keys before each test
        redisClient.connect().use { it.sync().flushall() }
    }

    fun sessionJson(expiresAt: Long = System.currentTimeMillis() + 3_600_000): String =
        Json.encodeToString(
            OAuthSessionData(accessToken = "tok", refreshToken = "ref", expiresAt = expiresAt)
        )

    describe("write and read") {
        it("reads back the written value") {
            val json = sessionJson()
            storage.write("r1", json)
            storage.read("r1") shouldBe json
        }
        it("overwrites an existing session") {
            storage.write("r2", sessionJson())
            val updated = sessionJson(System.currentTimeMillis() + 7_200_000)
            storage.write("r2", updated)
            storage.read("r2") shouldBe updated
        }
    }

    describe("read when missing") {
        it("throws NoSuchElementException for unknown id") {
            shouldThrow<NoSuchElementException> { storage.read("missing") }
        }
    }

    describe("invalidate") {
        it("removes the session") {
            storage.write("r3", sessionJson())
            storage.invalidate("r3")
            shouldThrow<NoSuchElementException> { storage.read("r3") }
        }
        it("does not throw when invalidating a non-existent id") {
            storage.invalidate("ghost")
        }
    }

    describe("TTL") {
        it("sets a positive TTL on the key") {
            storage.write("r4", sessionJson(System.currentTimeMillis() + 60_000))
            val ttl = redisClient.connect().sync().ttl("r4")
            (ttl > 0) shouldBe true
        }
    }
})
