// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric;

import com.google.gson.Gson;
import com.xpdustry.foundation.annotation.ScheduledTaskHandler;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.foundation.scheduler.MindustryTimeUnit;
import com.xpdustry.nucleus.concurrent.Async;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.database.PostgresDatabase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PostgresMetricRegistry implements MetricRegistry, PluginListener {

    private static final Logger log = LoggerFactory.getLogger(PostgresMetricRegistry.class);

    private final List<MetricCollector> collectors = new CopyOnWriteArrayList<>();
    private final ConfigManager config;
    private final ExecutorService executor;
    private final Gson gson;
    private final PostgresDatabase database;

    public PostgresMetricRegistry(
            final ConfigManager config,
            final ExecutorService executor,
            final Gson gson,
            final PostgresDatabase database) {
        this.config = config;
        this.executor = executor;
        this.gson = gson;
        this.database = database;
    }

    @Override
    public void register(final MetricCollector collector) {
        this.collectors.add(collector);
    }

    @ScheduledTaskHandler(initialDelay = 5, delay = 5, unit = MindustryTimeUnit.SECONDS)
    @Async
    private void onMetricsCollect() {
        if (!this.config.get(ConfigPropertyKey.METRICS_ENABLED)) {
            return;
        }
        final var server = this.config.get(ConfigPropertyKey.SERVER_NAME);
        final var now = Instant.now();
        final var sink = new BufferedMetricSink();
        final var tasks = this.collectors.stream()
                .map(collector -> Executors.callable(() -> {
                    try {
                        collector.flush(sink);
                    } catch (final Exception e) {
                        log.error("Failed to collect metrics from {}", collector, e);
                    }
                }))
                .toList();
        try {
            this.executor.invokeAll(tasks, 5, TimeUnit.SECONDS);
            sink.seal();
            this.database.withTransaction(handle -> handle.prepareStatement("""
                        INSERT INTO "metric" ("server_id", "measurement", "measured_at", "tags", "value")
                        VALUES (?, ?, ?, ?, ?)
                        """)
                    .batch(
                            sink.buffer,
                            (binder, sample) -> binder.bind(server)
                                    .bind(sample.name)
                                    .bind(now)
                                    .bind(this.gson.toJson(sample.labels))
                                    .bind(sample.value))
                    .executeUpdate());
        } catch (final Exception e) {
            log.error("Failed to collect and publish metrics", e);
        }
    }

    record MetricSample(String name, double value, Map<String, String> labels) {
        MetricSample {
            labels = Map.copyOf(labels);
        }
    }

    private static final class BufferedMetricSink implements MetricSink {

        private final List<MetricSample> buffer = new ArrayList<>();
        private boolean sealed = false;

        @Override
        public synchronized void sample(final String name, final Number value, final Map<String, String> labels) {
            if (this.sealed) throw new IllegalStateException("The sink is sealed");
            this.buffer.add(new MetricSample(name, value.doubleValue(), labels));
        }

        public synchronized void seal() {
            this.sealed = true;
        }
    }
}
