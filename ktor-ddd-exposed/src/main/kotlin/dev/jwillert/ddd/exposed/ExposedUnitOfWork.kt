package dev.jwillert.ddd.exposed

import dev.jwillert.ddd.UnitOfWork
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.slf4j.LoggerFactory

class ExposedUnitOfWork : UnitOfWork {
    private val logger = LoggerFactory.getLogger(ExposedUnitOfWork::class.java)

    override suspend operator fun <T> invoke(block: suspend () -> T): T {
        return try {
            suspendTransaction {
                block()
            }
        } catch (e: Exception) {
            logger.error("UnitOfWork failed", e)
            throw e
        }
    }
}
