package dev.jwillert.ktor.auth.storage

import io.ktor.server.sessions.SessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert

class ExposedSessionStorage(private val database: Database) : SessionStorage {

    private object SessionsTable : Table("oauth_sessions") {
        val sessionId = text("session_id")
        val data = text("data")
        val expiresAt = long("expires_at")
        override val primaryKey = PrimaryKey(sessionId)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(SessionsTable)
        }
    }

    override suspend fun read(id: String): String = withContext(Dispatchers.IO) {
        transaction(database) {
            val row = SessionsTable.selectAll()
                .where { SessionsTable.sessionId eq id }
                .singleOrNull()
                ?: throw NoSuchElementException("Session '$id' not found")

            if (row[SessionsTable.expiresAt] < System.currentTimeMillis()) {
                SessionsTable.deleteWhere { sessionId eq id }
                throw NoSuchElementException("Session '$id' has expired")
            }

            row[SessionsTable.data]
        }
    }

    override suspend fun write(id: String, value: String): Unit = withContext(Dispatchers.IO) {
        val expiresAt = extractExpiresAt(value)
        transaction(database) {
            SessionsTable.upsert {
                it[sessionId] = id
                it[data] = value
                it[this.expiresAt] = expiresAt
            }
        }
    }

    override suspend fun invalidate(id: String): Unit = withContext(Dispatchers.IO) {
        transaction(database) {
            SessionsTable.deleteWhere { sessionId eq id }
        }
    }

    /** Purges all expired sessions. Call periodically from a background job. */
    suspend fun purgeExpired(): Int = withContext(Dispatchers.IO) {
        transaction(database) {
            SessionsTable.deleteWhere { expiresAt less System.currentTimeMillis() }
        }
    }
}
