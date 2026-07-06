// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

import arc.Core;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.concurrent.NucleusExecutors;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.http.URIBuilder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// https://docs.influxdata.com/influxdb3/core/write-data/http-api/v3-write-lp/
// https://docs.influxdata.com/influxdb3/core/reference/line-protocol/
public final class MetricExporter implements PluginListener {

    private static final Logger log = LoggerFactory.getLogger(MetricExporter.class);
    private static final Pattern INFLUXDB_MEASUREMENT_ESCAPE_PATTERN = Pattern.compile("[\\\\ ,]");
    private static final Pattern INFLUXDB_TAG_ESCAPE_PATTERN = Pattern.compile("[\\\\ ,=]");

    private final List<MetricCollectorWithAsync> collectors = new CopyOnWriteArrayList<>();
    private final ConfigManager configManager;
    private final HttpClient httpClient;

    private final ThreadFactory workerFactory = NucleusExecutors.newVirtualThreadFactory("metric-exporter-worker");
    private final ScheduledExecutorService scheduler =
            NucleusExecutors.newSingleThreadScheduledExecutor("metric-exporter-scheduler");
    private @Nullable Future<?> scheduledTask = null;

    public MetricExporter(final ConfigManager configManager, final HttpClient httpClient) {
        this.configManager = configManager;
        this.httpClient = httpClient;
    }

    public void register(final MetricCollector collector) {
        this.register(collector, true);
    }

    public void register(final MetricCollector collector, final boolean async) {
        this.collectors.add(new MetricCollectorWithAsync(collector, async));
    }

    @Override
    public void onInit() {
        if (!this.configManager.get(ConfigPropertyKey.METRICS_INFLUXDB_ENABLED)) {
            return;
        }
        final var interval = Math.max(1, this.configManager.get(ConfigPropertyKey.METRICS_EXPORT_INTERVAL_SECONDS));
        this.scheduledTask =
                this.scheduler.scheduleWithFixedDelay(this::collectAndPublish, interval, interval, TimeUnit.SECONDS);
    }

    @Override
    public void onExit() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel(true);
        }
        this.scheduler.close();
    }

    @SuppressWarnings("preview")
    private void collectAndPublish() {
        final var server = this.configManager.get(ConfigPropertyKey.SERVER_NAME);
        final var now = Instant.now();
        final var timestamp = now.getEpochSecond() * 1_000_000_000L + now.getNano();

        try (final var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAll(),
                conf -> conf.withTimeout(Duration.ofSeconds(5L)).withThreadFactory(this.workerFactory))) {
            final var sink = new BufferingMetricSink();

            for (final var pair : this.collectors) {
                scope.fork(() -> {
                    if (pair.async || Core.app == null) {
                        pair.collector.flush(sink);
                    } else {
                        CompletableFuture.runAsync(() -> pair.collector.flush(sink), Core.app::post)
                                .join();
                    }
                });
            }
            scope.join();

            final var samples = new ArrayList<>(sink.samples);
            samples.add(new MetricSample("metric_samples_count", MetricType.GAUGE, samples.size(), Map.of()));

            final var lines = new StringBuilder();

            for (final var sample : samples) {
                if (!Double.isFinite(sample.value)) {
                    continue;
                }

                lines.append(escapeInfluxMeasurement(sample.name));
                lines.append(",server=").append(escapeInfluxTag(server));
                lines.append(",metric_type=").append(sample.type.name().toLowerCase(Locale.ROOT));
                for (final var label : sample.labels.entrySet()) {
                    lines.append(',')
                            .append(escapeInfluxTag(label.getKey()))
                            .append('=')
                            .append(escapeInfluxTag(label.getValue()));
                }

                lines.append(" value=").append(sample.value);
                lines.append(' ').append(timestamp).append('\n');
            }

            if (lines.isEmpty()) {
                return;
            }

            final var uri = new URIBuilder(this.configManager.get(ConfigPropertyKey.METRICS_INFLUXDB_ENDPOINT))
                    .addPathSegment("api")
                    .addPathSegment("v3")
                    .addPathSegment("write_lp")
                    .addParameter("precision", "nanosecond")
                    .addParameter("accept_partial", "false")
                    .addParameter("db", this.configManager.get(ConfigPropertyKey.METRICS_INFLUXDB_DATABASE))
                    .build();

            final var request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10L))
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .header(
                            "Authorization",
                            "Token " + this.configManager.get(ConfigPropertyKey.METRICS_INFLUXDB_TOKEN))
                    .POST(HttpRequest.BodyPublishers.ofString(lines.toString()))
                    .build();

            final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("InfluxDB rejected metrics with status {}: {}", response.statusCode(), response.body());
            }
        } catch (final Exception e) {
            log.error("Failed to collect and publish metrics", e);
        }
    }

    private static String escapeInfluxMeasurement(final String value) {
        return INFLUXDB_MEASUREMENT_ESCAPE_PATTERN.matcher(value).replaceAll("\\\\$0");
    }

    private static String escapeInfluxTag(final String value) {
        return INFLUXDB_TAG_ESCAPE_PATTERN.matcher(value).replaceAll("\\\\$0");
    }

    record MetricSample(String name, MetricType type, double value, Map<String, String> labels) {
        MetricSample {
            labels = Map.copyOf(labels);
        }
    }

    private static final class BufferingMetricSink implements MetricSink {

        private final List<MetricSample> samples = new ArrayList<>();

        @Override
        public synchronized void sample(
                final String name, final MetricType type, final Number value, final Map<String, String> labels) {
            this.samples.add(new MetricSample(name, type, value.doubleValue(), labels));
        }
    }

    private record MetricCollectorWithAsync(MetricCollector collector, boolean async) {}
}
