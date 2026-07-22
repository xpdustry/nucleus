// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database

import com.xpdustry.foundation.plugin.PluginListener
import com.xpdustry.nucleus.config.ConfigManager
import com.xpdustry.nucleus.config.ConfigPropertyKey
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.io.IOException
import java.nio.file.Path
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.use
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mindustry.Vars
import org.intellij.lang.annotations.Language
import org.postgresql.PGConnection
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory

internal class PostgresDatabaseImpl(
    private val config: ConfigManager,
    private val directory: Path,
    private val scope: CoroutineScope,
) : PostgresDatabase, PluginListener {
    private lateinit var factory: PostgresDataSourceFactory
    private lateinit var source: HikariDataSource

    override fun onInit() {
        if (this.config.get(ConfigPropertyKey.DATABASE_EMBEDDED)) {
            check(Vars.mods == null || Vars.mods.getMod("sql4md-postgresql-embedded") != null) {
                "The 'sql4md-postgresql-embedded' plugin is missing, cannot use a local postgres instance without it"
            }
            this.factory = EmbeddedPostgresDataSourceFactory(this.directory)
        } else {
            this.factory =
                ExternalPostgresDataSourceFactory(
                    this.config.get(ConfigPropertyKey.DATABASE_HOST),
                    this.config.get(ConfigPropertyKey.DATABASE_PORT),
                    this.config.get(ConfigPropertyKey.DATABASE_NAME),
                    this.config.get(ConfigPropertyKey.DATABASE_USERNAME),
                    this.config.get(ConfigPropertyKey.DATABASE_PASSWORD),
                )
        }
        try {
            this.factory.init()
        } catch (e: IOException) {
            throw RuntimeException("Failed to init the postgres data source factory", e)
        }

        val config = HikariConfig()
        config.setDataSource(this.factory.create())
        config.setPoolName("sql-transaction-pool")
        config.setMaximumPoolSize((Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(2))
        config.setMinimumIdle(2)
        config.setAutoCommit(false)
        this.source = HikariDataSource(config)

        runBlocking {
            this@PostgresDatabaseImpl.transaction {
                for (statement in SETUP_SCRIPT.split(';')) {
                    statement.asPreparedStatement().executeUpdate()
                }
            }
        }
    }

    override fun onExit() {
        source.close()
        factory.exit()
    }

    override suspend fun <R> transaction(block: suspend PostgresDatabase.Handle.() -> R): R =
        withContext(Dispatchers.IO) {
            val element = currentCoroutineContext()[CoroutineHandleElement]
            if (element != null && element.owner == this@PostgresDatabaseImpl) {
                return@withContext block(element.handle)
            }
            return@withContext this@PostgresDatabaseImpl.source.connection.use { connection ->
                val handle = HandleImpl(connection)
                try {
                    handle.connection.autoCommit = false
                    handle.connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
                    val result =
                        withContext(CoroutineHandleElement(handle, this@PostgresDatabaseImpl)) { block(handle) }
                    handle.connection.commit()
                    return@use result
                } catch (e: SQLException) {
                    handle.connection.rollback()
                    throw e
                }
            }
        }

    @Suppress("SqlSourceToSinkFlow")
    override fun listen(channel: String, onNotification: suspend (String) -> Unit): Job {
        require(POSTGRES_IDENTIFIER.matches(channel)) { "Invalid PostgreSQL channel name: $channel" }
        return scope.launch(Dispatchers.IO) {
            while (isActive) try {
                this@PostgresDatabaseImpl.source.connection.use { connection ->
                    connection.autoCommit = true
                    connection.createStatement().use { it.execute("LISTEN \"$channel\"") }
                    val postgres = connection.unwrap(PGConnection::class.java)
                    while (isActive) {
                        for (notification in postgres.getNotifications(1000)) {
                            if (notification.name != channel) continue
                            scope.launch { onNotification(notification.parameter) }
                        }
                    }
                }
            } catch (e: SQLException) {
                log.error("An error occurred while listening to $channel", e)
                delay(10.seconds)
            }
        }
    }
}

private val log = LoggerFactory.getLogger(PostgresDatabaseImpl::class.java)

private val POSTGRES_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

private class PreparedStatementBuilderImpl(private val statement: PreparedStatement) :
    PostgresDatabase.PreparedStatementBuilder {
    private var index = 1

    override fun push(value: String): PostgresDatabase.PreparedStatementBuilder {
        statement.setString(index, value)
        index++
        return this
    }

    override fun push(value: Int): PostgresDatabase.PreparedStatementBuilder {
        statement.setInt(index, value)
        index++
        return this
    }

    override fun push(value: Long): PostgresDatabase.PreparedStatementBuilder {
        statement.setLong(index, value)
        index++
        return this
    }

    override fun push(value: Boolean): PostgresDatabase.PreparedStatementBuilder {
        statement.setBoolean(index, value)
        index++
        return this
    }

    override fun push(value: Double): PostgresDatabase.PreparedStatementBuilder {
        statement.setDouble(index, value)
        index++
        return this
    }

    override fun push(value: Float): PostgresDatabase.PreparedStatementBuilder {
        statement.setFloat(index, value)
        index++
        return this
    }

    override fun push(value: ByteArray): PostgresDatabase.PreparedStatementBuilder {
        statement.setBytes(index, value)
        index++
        return this
    }

    override fun push(value: Instant): PostgresDatabase.PreparedStatementBuilder {
        statement.setTimestamp(index, Timestamp.from(value.toJavaInstant()))
        index++
        return this
    }

    override suspend fun <T> executeSelect(mapper: PostgresDatabase.Row.() -> T) = statement.use { statement ->
        statement.executeQuery().use { result ->
            val view = RowImpl(result)
            val list = ArrayList<T>()
            while (result.next()) list += view.mapper()
            return@use list
        }
    }

    override suspend fun <T> executeSingleSelect(mapper: PostgresDatabase.Row.() -> T) = statement.use { statement ->
        statement.executeQuery().use { result ->
            val view = RowImpl(result)
            val value: T
            if (result.next()) {
                value = view.mapper()
                if (result.next()) {
                    // TODO Change message
                    error("That ain't supposed to happen.")
                }
            } else {
                // TODO Change message
                error("That ain't supposed to happen.")
            }
            return@use value
        }
    }

    override suspend fun executeUpdate(): Int = statement.use { statement -> statement.executeUpdate() }

    override suspend fun executeSingleUpdate() =
        when (val result = this.executeUpdate()) {
            0 -> false
            1 -> true
            else -> error("Multiple rows updated, expected 0 or 1, got $result")
        }
}

private data class HandleImpl(val connection: Connection) : PostgresDatabase.Handle {
    override fun String.asPreparedStatement() = PreparedStatementBuilderImpl(connection.prepareStatement(this))
}

private class RowImpl(private val set: ResultSet) : PostgresDatabase.Row {
    override fun getString(name: String): String? = set.getString(name)

    override fun getInt(name: String): Int? {
        val value = set.getInt(name)
        return if (set.wasNull()) null else value
    }

    override fun getLong(name: String): Long? {
        val value = set.getLong(name)
        return if (set.wasNull()) null else value
    }

    override fun getBoolean(name: String): Boolean? {
        val value = set.getBoolean(name)
        return if (set.wasNull()) null else value
    }

    override fun getDouble(name: String): Double? {
        val value = set.getDouble(name)
        return if (set.wasNull()) null else value
    }

    override fun getFloat(name: String): Float? {
        val value = set.getFloat(name)
        return if (set.wasNull()) null else value
    }

    override fun getBytes(name: String): ByteArray? = set.getBytes(name)

    override fun getInstant(name: String): Instant? =
        set.getTimestamp(name)?.let { Instant.fromEpochSeconds(it.time, it.nanos) }
}

private class CoroutineHandleElement(val handle: HandleImpl, val owner: PostgresDatabaseImpl) :
    AbstractCoroutineContextElement(CoroutineHandleElement) {
    companion object Key : CoroutineContext.Key<CoroutineHandleElement>
}

private interface PostgresDataSourceFactory {
    fun init() = Unit

    fun exit() = Unit

    fun create(): DataSource?
}

private data class ExternalPostgresDataSourceFactory(
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
) : PostgresDataSourceFactory {
    override fun create(): DataSource {
        val source = PGSimpleDataSource()
        source.setUrl("jdbc:postgresql://${this.host}:${this.port}/${this.database}")
        source.user = this.username
        source.password = this.password
        return source
    }
}

private class EmbeddedPostgresDataSourceFactory(private val directory: Path) : PostgresDataSourceFactory {
    private lateinit var embedded: EmbeddedPostgres

    override fun init() {
        this.embedded =
            EmbeddedPostgres.builder()
                .setDataDirectory(this.directory)
                .setCleanDataDirectory(false)
                .setRegisterShutdownHook(true)
                .start()
    }

    override fun exit() {
        this.embedded.close()
    }

    override fun create(): DataSource {
        return this.embedded.postgresDatabase
    }
}

@Language("PostgreSQL")
private val SETUP_SCRIPT =
    """
    CREATE TABLE IF NOT EXISTS "user" (
        "id"            INTEGER     NOT NULL
            GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
        "uuid"          BIGINT      NOT NULL
            UNIQUE
    );

    CREATE TABLE IF NOT EXISTS "user_game_session" (
        "user_id"       INTEGER     NOT NULL
            REFERENCES "user" ("id")
                ON DELETE CASCADE,
        "server_id"     VARCHAR(64) NOT NULL,
        "name"          VARCHAR(64) NOT NULL,
        "address"       INET        NOT NULL,
        "started_at"    TIMESTAMP   NOT NULL,
        "ended_at"      TIMESTAMP   NOT NULL
    );

    CREATE TABLE IF NOT EXISTS "address_whitelist" (
        "address"       INET        NOT NULL
            PRIMARY KEY,
        "reason"        TEXT        NOT NULL,
        "added_at"      TIMESTAMP   NOT NULL
            DEFAULT current_timestamp
    );

    CREATE UNLOGGED TABLE IF NOT EXISTS "address_info_request_cache" (
        "address"       INET        NOT NULL
            PRIMARY KEY,
        "safe"          BOOLEAN     NOT NULL,
        "added_at"      TIMESTAMP   NOT NULL
            DEFAULT current_timestamp,
        "updated_at"    TIMESTAMP   NOT NULL
            DEFAULT current_timestamp,
        "ttl"           INTERVAL    NOT NULL
            DEFAULT INTERVAL '24 hours',
        "country_code"  VARCHAR(3)  NOT NULL,
        "asn_name"      VARCHAR(64) NOT NULL,
        "asn_number"    BIGINT      NOT NULL
    );
    """
        .trimIndent()
