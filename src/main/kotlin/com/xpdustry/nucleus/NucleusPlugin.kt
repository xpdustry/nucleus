// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus

import com.google.gson.Gson
import com.xpdustry.foundation.annotation.PluginAnnotationProcessor
import com.xpdustry.foundation.plugin.BaseMindustryPlugin
import com.xpdustry.nucleus.config.ConfigManager
import com.xpdustry.nucleus.database.PostgresDatabaseImpl
import com.xpdustry.nucleus.gatekeeper.GatekeeperController
import com.xpdustry.nucleus.gatekeeper.GatekeeperPipeline
import com.xpdustry.nucleus.message.MessagePublisher
import com.xpdustry.nucleus.metric.MetricExporter
import com.xpdustry.nucleus.metric.MindustryMetricCollector
import com.xpdustry.nucleus.network.InetAddressInfoProvider
import com.xpdustry.nucleus.network.InetAddressWhitelist
import com.xpdustry.nucleus.text.BadWordFinder
import java.net.http.HttpClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NucleusPlugin : BaseMindustryPlugin() {
    val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default +
                CoroutineName("NucleusScope") +
                CoroutineExceptionHandler { _, throwable ->
                    logger().error("Unhandled exception in coroutine", throwable)
                }
        )

    private val processor: PluginAnnotationProcessor<*> =
        PluginAnnotationProcessor.compose(
            PluginAnnotationProcessor.events(this),
            PluginAnnotationProcessor.scheduledTasks(this),
            PluginAnnotationProcessor.playerActions(this),
        )

    private val configManager = this.addListener(ConfigManager(this.directory().resolve("config.properties")))

    private val gson = Gson()
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    private val database =
        this.addListener(
            PostgresDatabaseImpl(
                this.configManager,
                this.directory().resolve("postgres"),
                scope,
            )
        )

    private val network = this.addListener(MessagePublisher(this.configManager, this.database))

    private val badWords = this.addListener(BadWordFinder())

    private val addressInfoProvider =
        this.addListener(InetAddressInfoProvider(this.configManager, this.gson, this.httpClient, this.database))
    private val addressWhitelist = InetAddressWhitelist(this.database)

    private val gatekeeperPipeline = GatekeeperPipeline()

    private val metrics = this.addListener(MetricExporter(this.configManager, this.httpClient))

    override fun onInit() {
        this.metrics.register(this.addListener(MindustryMetricCollector()), false)
        this.addListener(
            GatekeeperController(
                this.gatekeeperPipeline,
                this.configManager,
                this.badWords,
                this.addressInfoProvider,
                this.addressWhitelist,
                scope,
            )
        )
        for (listener in this.listeners()) {
            this.processor.process(listener)
        }
    }
}
