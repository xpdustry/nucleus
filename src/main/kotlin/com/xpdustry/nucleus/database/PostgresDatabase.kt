// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database

import kotlin.ByteArray
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.time.Instant
import kotlinx.coroutines.Job
import org.intellij.lang.annotations.Language

interface PostgresDatabase {
    suspend fun <R> transaction(block: suspend Handle.() -> R): R

    fun listen(channel: String, onNotification: suspend (String) -> Unit): Job

    interface Handle {
        fun @receiver:Language("SQL") String.asPreparedStatement(): PreparedStatementBuilder
    }

    interface PreparedStatementBuilder {
        fun push(value: String): PreparedStatementBuilder

        fun push(value: Int): PreparedStatementBuilder

        fun push(value: Long): PreparedStatementBuilder

        fun push(value: Boolean): PreparedStatementBuilder

        fun push(value: Double): PreparedStatementBuilder

        fun push(value: Float): PreparedStatementBuilder

        fun push(value: ByteArray): PreparedStatementBuilder

        fun push(value: Instant): PreparedStatementBuilder

        suspend fun <T> executeSelect(mapper: Row.() -> T): List<T>

        suspend fun <T> executeSingleSelect(mapper: Row.() -> T): T

        suspend fun executeUpdate(): Int

        suspend fun executeSingleUpdate(): Boolean
    }

    interface Row {
        fun getString(name: String): String?

        fun getInt(name: String): Int?

        fun getLong(name: String): Long?

        fun getBoolean(name: String): Boolean?

        fun getDouble(name: String): Double?

        fun getFloat(name: String): Float?

        fun getBytes(name: String): ByteArray?

        fun getInstant(name: String): Instant?
    }
}
