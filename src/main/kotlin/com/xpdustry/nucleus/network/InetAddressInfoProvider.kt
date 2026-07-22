// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network

import com.google.gson.Gson
import com.xpdustry.foundation.annotation.ScheduledTaskHandler
import com.xpdustry.foundation.plugin.PluginListener
import com.xpdustry.foundation.scheduler.MindustryTimeUnit
import com.xpdustry.nucleus.config.ConfigManager
import com.xpdustry.nucleus.config.ConfigPropertyKey
import com.xpdustry.nucleus.database.PostgresDatabase
import com.xpdustry.nucleus.http.URIBuilder
import java.net.InetAddress
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.Boolean
import kotlin.Exception
import kotlin.String
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InetAddressInfoProvider(
    private val configManager: ConfigManager,
    private val gson: Gson,
    private val http: HttpClient,
    private val database: PostgresDatabase,
) : PluginListener {
    suspend fun get(address: InetAddress): InetAddressInfo? {
        val cached =
            this.database.transaction {
                """
                SELECT a."safe" as "safe", a."country_code" as "country_code", a."asn_name" as "asn_name", a."asn_number" as "asn_number"
                FROM "address_info_request_cache" a
                WHERE a."address" = ?::inet AND a."updated_at" + a."ttl" >= current_timestamp
                """
                    .asPreparedStatement()
                    .push(address.hostAddress)
                    .executeSelect {
                        InetAddressInfo(
                            getBoolean("safe")!!,
                            getString("country_code")!!,
                            getString("asn_same")!!,
                            getLong("asn_number")!!,
                        )
                    }
                    .firstOrNull()
            }
        if (cached != null) {
            return cached
        }

        val token = this.configManager.get(ConfigPropertyKey.VPN_API_IO_TOKEN)
        if (token.isEmpty()) {
            return null
        }

        val response: HttpResponse<String>
        try {
            response =
                withContext(Dispatchers.IO) {
                    this@InetAddressInfoProvider.http.send(
                        HttpRequest.newBuilder(
                                URIBuilder("https://vpnapi.io/api/")
                                    .addPathSegment(address.hostAddress)
                                    .addParameter("key", token)
                                    .build()
                            )
                            .GET()
                            .build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                }
        } catch (e: Exception) {
            log.error("An error occurred while retrieving info about {} from vpnapi.io", address.hostAddress, e)
            return null
        }

        val code = response.statusCode()
        if (code != 200) {
            log.error(
                "An unexpected response has been received from vpnapi.io while retrieving info about {} (status_code={}, body={})",
                address.hostAddress,
                code,
                response.body(),
            )
            return null
        }

        try {
            val body: VpnApiIoResponse =
                this.gson.fromJson<VpnApiIoResponse>(response.body(), VpnApiIoResponse::class.java)
            val safe = body.security.vpn || body.security.proxy || body.security.tor || body.security.relay
            val countryCode = body.location.country_code
            val asnName = body.network.autonomous_system_organization
            val asnNumber = body.network.autonomous_system_number.replaceFirst("AS", "").toLong()

            this.database.transaction {
                """
                INSERT INTO "address_info_request_cache"("address", "safe", "country_code", "asn_name", "asn_number")
                VALUES (?::inet, ?, ?, ?, ?)
                ON CONFLICT ("address") DO UPDATE SET
                    "updated_at"    = current_timestamp,
                    "safe"          = excluded."safe",
                    "country_code"  = excluded."country_code",
                    "asn_name"      = excluded."asn_name",
                    "asn_number"    = excluded."asn_number"

                """
                    .asPreparedStatement()
                    .push(address.hostAddress)
                    .push(safe)
                    .push(countryCode)
                    .push(asnName)
                    .push(asnNumber)
                    .executeSingleUpdate()
            }

            return InetAddressInfo(safe, countryCode, asnName, asnNumber)
        } catch (e: Exception) {
            log.error(
                "Failed to deserialize the response from vpnapi.io while retrieving info about {} (json={})",
                address.hostAddress,
                response.body(),
                e,
            )
            return null
        }
    }

    // TODO Add error handling + better logging
    @ScheduledTaskHandler(initialDelay = 0, delay = 12, unit = MindustryTimeUnit.HOURS)
    suspend fun housekeeping() {
        this.database.transaction {
            """
            DELETE FROM "address_info_request_cache" a
            WHERE a."updated_at" + a."ttl" < current_timestamp
            """
                .asPreparedStatement()
                .executeUpdate()
        }
    }

    // https://vpnapi.io/api-documentation
    @JvmRecord
    private data class VpnApiIoResponse(val security: Security, val location: Location, val network: Network) {
        @JvmRecord
        internal data class Security(val vpn: Boolean, val proxy: Boolean, val tor: Boolean, val relay: Boolean)

        @JvmRecord internal data class Location(val country_code: String)

        @JvmRecord
        internal data class Network(val autonomous_system_number: String, val autonomous_system_organization: String)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(InetAddressInfoProvider::class.java)
    }
}
