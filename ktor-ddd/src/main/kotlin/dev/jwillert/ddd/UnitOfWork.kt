package dev.jwillert.ddd

interface UnitOfWork {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}
