// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric

import arc.Core
import com.xpdustry.foundation.plugin.PluginListener
import com.xpdustry.nucleus.concurrent.NucleusExecutors
import com.xpdustry.nucleus.config.ConfigManager
import com.xpdustry.nucleus.config.ConfigPropertyKey
import com.xpdustry.nucleus.http.URIBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.*
import java.util.function.Function
import java.util.regex.Pattern
import kotlin.Any
import kotlin.Boolean
import kotlin.Exception
import kotlin.Int
import kotlin.Number
import kotlin.String
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.getValue
import kotlin.getValue
import kotlin.use
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// https://docs.influxdata.com/influxdb3/core/write-data/http-api/v3-write-lp/
// https://docs.influxdata.com/influxdb3/core/reference/line-protocol/
class MetricExporter(private val configManager: ConfigManager, private val httpClient: HttpClient) : PluginListener {
    private val collectors: MutableList<MetricCollectorWithAsync> = CopyOnWriteArrayList<MetricCollectorWithAsync>()

    private val workerFactory: ThreadFactory = NucleusExecutors.newVirtualThreadFactory("metric-exporter-worker")
    private val scheduler: ScheduledExecutorService =
        NucleusExecutors.newSingleThreadScheduledExecutor("metric-exporter-scheduler")
    private var scheduledTask: Future<*>? = null

    @JvmOverloads
    fun register(collector: MetricCollector, async: Boolean = true) {
        this.collectors.add(MetricCollectorWithAsync(collector, async))
    }

    override fun onInit() {
        if (!this.configManager.get<Boolean>(ConfigPropertyKey.Companion.METRICS_INFLUXDB_ENABLED)) {
            return
        }
        val interval =
            Math.max(1, this.configManager.get<Int>(ConfigPropertyKey.Companion.METRICS_EXPORT_INTERVAL_SECONDS))
        this.scheduledTask =
            this.scheduler.scheduleWithFixedDelay(
                Runnable { this.collectAndPublish() },
                interval.toLong(),
                interval.toLong(),
                TimeUnit.SECONDS,
            )
    }

    override fun onExit() {
        if (this.scheduledTask != null) {
            this.scheduledTask!!.cancel(true)
        }
        this.scheduler.close()
    }

    private fun collectAndPublish() {
        val server = this.configManager.get<String>(ConfigPropertyKey.Companion.SERVER_NAME)
        val now = Instant.now()
        val timestamp = now.getEpochSecond() * 1000000000L + now.getNano()

        try {
            StructuredTaskScope.open<Any, Void>(
                    StructuredTaskScope.Joiner.awaitAll<Any>(),
                    Function { conf: StructuredTaskScope.Configuration? ->
                        conf!!.withTimeout(Duration.ofSeconds(5L)).withThreadFactory(this.workerFactory)
                    },
                )
                .use { scope ->
                    val sink = BufferingMetricSink()
                    for (pair in this.collectors) {
                        scope.fork<Any>(
                            Runnable {
                                if (pair.async || Core.app == null) {
                                    pair.collector.flush(sink)
                                } else {
                                    CompletableFuture.runAsync(
                                            Runnable { pair.collector.flush(sink) },
                                            Executor { runnable: Runnable? -> Core.app.post(runnable) },
                                        )
                                        .join()
                                }
                            }
                        )
                    }
                    scope.join()

                    val samples = ArrayList<MetricSample>(sink.samples)
                    samples.add(
                        MetricSample(
                            "metric_samples_count",
                            MetricType.GAUGE,
                            samples.size.toDouble(),
                            emptyMap(),
                        )
                    )

                    val lines = StringBuilder()

                    for (sample in samples) {
                        if (!sample.value.isFinite()) {
                            continue
                        }

                        lines.append(escapeInfluxMeasurement(sample.name))
                        lines.append(",server=").append(escapeInfluxTag(server))
                        lines.append(",metric_type=").append(sample.type.name.lowercase(Locale.ROOT))
                        for ((name, value) in sample.labels) {
                            lines.append(',').append(escapeInfluxTag(name)).append('=').append(escapeInfluxTag(value))
                        }

                        lines.append(" value=").append(sample.value)
                        lines.append(' ').append(timestamp).append('\n')
                    }

                    if (lines.isEmpty()) {
                        return
                    }

                    val uri =
                        URIBuilder(this.configManager.get<URI>(ConfigPropertyKey.Companion.METRICS_INFLUXDB_ENDPOINT))
                            .addPathSegment("api")
                            .addPathSegment("v3")
                            .addPathSegment("write_lp")
                            .addParameter("precision", "nanosecond")
                            .addParameter("accept_partial", "false")
                            .addParameter(
                                "db",
                                this.configManager.get<String>(ConfigPropertyKey.Companion.METRICS_INFLUXDB_DATABASE),
                            )
                            .build()

                    val request =
                        HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(10L))
                            .header("Content-Type", "text/plain; charset=utf-8")
                            .header(
                                "Authorization",
                                "Token " +
                                    this.configManager.get<String>(ConfigPropertyKey.Companion.METRICS_INFLUXDB_TOKEN),
                            )
                            .POST(HttpRequest.BodyPublishers.ofString(lines.toString()))
                            .build()

                    val response = this.httpClient.send<String>(request, HttpResponse.BodyHandlers.ofString())
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        log.error(
                            "InfluxDB rejected metrics with status {}: {}",
                            response.statusCode(),
                            response.body(),
                        )
                    }
                }
        } catch (e: Exception) {
            log.error("Failed to collect and publish metrics", e)
        }
    }

    internal data class MetricSample(
        val name: String,
        val type: MetricType,
        val value: kotlin.Double,
        val labels: Map<String, String>,
    )

    private class BufferingMetricSink : MetricSink {
        val samples: MutableList<MetricSample> = ArrayList<MetricSample>()

        @Synchronized
        override fun sample(
            name: String,
            type: MetricType,
            value: Number,
            labels: Map<String, String>,
        ) {
            this.samples.add(MetricSample(name, type, value.toDouble(), labels.toMap()))
        }
    }

    @JvmRecord private data class MetricCollectorWithAsync(val collector: MetricCollector, val async: Boolean)

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MetricExporter::class.java)
        private val INFLUXDB_MEASUREMENT_ESCAPE_PATTERN: Pattern = Pattern.compile("[\\\\ ,]")
        private val INFLUXDB_TAG_ESCAPE_PATTERN: Pattern = Pattern.compile("[\\\\ ,=]")

        private fun escapeInfluxMeasurement(value: String): String {
            return INFLUXDB_MEASUREMENT_ESCAPE_PATTERN.matcher(value).replaceAll("\\\\$0")
        }

        private fun escapeInfluxTag(value: String): String {
            return INFLUXDB_TAG_ESCAPE_PATTERN.matcher(value).replaceAll("\\\\$0")
        }
    }
}
