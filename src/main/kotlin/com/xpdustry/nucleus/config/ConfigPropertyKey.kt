// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config

import com.xpdustry.nucleus.gatekeeper.GatekeeperFailurePolicy
import java.net.URI
import java.util.Collections
import kotlin.reflect.KClass

data class ConfigPropertyKey<T : Any>(val name: String, val type: KClass<T>, val def: T) {
    companion object {
        private val ALL = HashMap<String, ConfigPropertyKey<*>>()

        // --- Server wide configs ------------------------------------------------
        val SERVER_NAME = registering<String>("nucleus.server.name", "unknown")

        val SERVER_DISCORD = registering<URI>("nucleus.server.discord", URI("https://discord.xpdustry.com"))

        // --- Database -----------------------------------------------------------
        val DATABASE_HOST = registering<String>("nucleus.database.host", "localhost")

        val DATABASE_PORT = registering<Int>("nucleus.database.port", 5432)

        val DATABASE_NAME = registering<String>("nucleus.database.name", "postgres")

        val DATABASE_USERNAME = registering<String>("nucleus.database.username", "postgres")

        val DATABASE_PASSWORD = registering<String>("nucleus.database.password", "postgres")

        val DATABASE_EMBEDDED = registering<Boolean>("nucleus.database.embedded", true)

        // --- Network ------------------------------------------------------------
        val VPN_API_IO_TOKEN: ConfigPropertyKey<String> = registering<String>("nucleus.vpn_api_io_token", "")

        // TODO Change when MindustryUserRepo implemented?
        val GATEKEEPER_FAILURE_POLICY =
            registering<GatekeeperFailurePolicy>("nucleus.gatekeeper.failure_policy", GatekeeperFailurePolicy.ALLOW_ALL)

        // --- Metrics ------------------------------------------------------------
        val METRICS_INFLUXDB_ENABLED = registering<Boolean>("nucleus.metrics.influxdb.enabled", false)

        val METRICS_INFLUXDB_ENDPOINT =
            registering<URI>("nucleus.metrics.influxdb.endpoint", URI.create("http://localhost:8086"))

        val METRICS_INFLUXDB_TOKEN: ConfigPropertyKey<String> =
            registering<String>("nucleus.metrics.influxdb.token", "")

        val METRICS_INFLUXDB_DATABASE: ConfigPropertyKey<String> =
            registering<String>("nucleus.metrics.influxdb.database", "nucleus")

        val METRICS_EXPORT_INTERVAL_SECONDS: ConfigPropertyKey<Int> =
            registering<Int>("nucleus.metrics.influxdb.export_interval_seconds", 5)

        // --- E.N.D --------------------------------------------------------------
        fun all(): MutableMap<String, ConfigPropertyKey<*>> {
            return Collections.unmodifiableMap(ALL)
        }

        private inline fun <reified T : Any> registering(name: String, def: T): ConfigPropertyKey<T> {
            val key = ConfigPropertyKey(name, T::class, def)
            check(!ALL.containsKey(key.name)) { "Duplicate key " + key.name }
            ALL[key.name] = key
            return key
        }
    }
}
