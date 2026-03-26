package dev.jwillert.ktor.auth.storage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InMemorySessionStorageSpec : DescribeSpec({

    lateinit var storage: InMemorySessionStorage

    beforeEach { storage = InMemorySessionStorage() }

    describe("write and read") {
        it("reads back the written value") {
            storage.write("id1", "data1")
            storage.read("id1") shouldBe "data1"
        }
        it("overwrites an existing value") {
            storage.write("id1", "first")
            storage.write("id1", "second")
            storage.read("id1") shouldBe "second"
        }
    }

    describe("read when missing") {
        it("throws NoSuchElementException for unknown id") {
            shouldThrow<NoSuchElementException> { storage.read("unknown") }
        }
    }

    describe("invalidate") {
        it("removes the session") {
            storage.write("id2", "data")
            storage.invalidate("id2")
            shouldThrow<NoSuchElementException> { storage.read("id2") }
        }
        it("does not throw when invalidating a non-existent id") {
            storage.invalidate("ghost") // should not throw
        }
    }

    describe("concurrency") {
        it("handles concurrent writes without data loss") {
            val ids = (1..100).map { "id-$it" }
            withContext(Dispatchers.Default) {
                ids.forEach { id ->
                    launch { storage.write(id, "value-$id") }
                }
            }
            ids.forEach { id -> storage.read(id) shouldBe "value-$id" }
        }
    }
})
