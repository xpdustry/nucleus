// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network

import com.xpdustry.nucleus.database.PostgresDatabase
import java.net.InetAddress
import kotlin.String

class InetAddressWhitelist(private val database: PostgresDatabase) {
    suspend fun contains(address: InetAddress): Boolean {
        return this.database.transaction {
            """
                SELECT 1 FROM "address_whitelist" "a"
                WHERE ?::inet <<= "a"."address" LIMIT 1
                """
                .asPreparedStatement()
                .push(address.hostAddress)
                .executeSingleSelect { true }
        }
    }

    suspend fun insert(address: InetAddress, reason: String) {
        this.database.transaction {
            """
            INSERT INTO "address_whitelist"("address", "reason") VALUES (?, ?)
            """
                .asPreparedStatement()
                .push(address.hostAddress)
                .push(reason)
                .executeSingleUpdate()
        }
    }
}
