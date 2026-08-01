// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

import com.google.gson.Gson;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.concurrent.NucleusExecutors;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.database.PostgresDatabase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PostgresMetricExporter implements MetricRegistry, PluginListener {

    private static final Logger log = LoggerFactory.getLogger(PostgresMetricExporter.class);

    private final List<MetricCollector> collectors = new CopyOnWriteArrayList<>();
    private final ConfigManager config;
    private final Gson gson;
    private final PostgresDatabase database;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler =
            NucleusExecutors.newSingleThreadScheduledExecutor("metric-exporter-scheduler");
    private @Nullable Future<?> scheduledTask = null;

    public PostgresMetricExporter(
            final ConfigManager config,
            final Gson gson,
            final PostgresDatabase database,
            final ExecutorService executor) {
        this.config = config;
        this.gson = gson;
        this.database = database;
        this.executor = executor;
    }

    @Override
    public void register(final MetricCollector collector) {
        this.collectors.add(collector);
    }

    private void collectAndPublish() {
        final var server = this.config.get(ConfigPropertyKey.SERVER_NAME);
        final var now = Instant.now();
        final var sink = new BufferingMetricSink();
        final var tasks = this.collectors.stream()
                .map(collector -> (Callable<@Nullable Void>) () -> {
                    collector.flush(sink);
                    return null;
                })
                .toList();
        try {
            this.executor.invokeAll(tasks, 5, TimeUnit.SECONDS);
            this.database.withHandle(handle -> {
                final var builder = handle.prepareStatement("""
                        INSERT INTO "metric" ("server_id", "measurement", "measured_at", "tags", "value")
                        VALUES (?, ?, ?, ?, ?)
                        """);
                for (final var sample : sink.samples) {
                    builder.push(server)
                            .push(sample.name)
                            .push(now)
                            .push(this.gson.toJson(sample.labels))
                            .push(sample.value)
                            .addToBatch();
                }
                return builder.executeUpdate();
            });
        } catch (final Exception e) {
            log.error("Failed to collect and publish metrics", e);
        }
    }

    @Override
    public void onInit() {
        if (!this.config.get(ConfigPropertyKey.METRICS_ENABLED)) {
            return;
        }
        final var interval = Math.max(1, this.config.get(ConfigPropertyKey.METRICS_COLLECTION_INTERVAL_SECONDS));
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

    record MetricSample(String name, double value, Map<String, String> labels) {
        MetricSample {
            labels = Map.copyOf(labels);
        }
    }

    // TODO Implement actual locking so it throws if modified outside the collector scope
    private static final class BufferingMetricSink implements MetricSink {

        private final List<MetricSample> samples = new ArrayList<>();

        @Override
        public synchronized void sample(final String name, final Number value, final Map<String, String> labels) {
            this.samples.add(new MetricSample(name, value.doubleValue(), labels));
        }
    }
}
