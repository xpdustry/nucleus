// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus;

import com.google.gson.Gson;
import com.xpdustry.foundation.annotation.PluginAnnotationProcessor;
import com.xpdustry.foundation.plugin.BaseMindustryPlugin;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.foundation.scheduler.MindustryTask;
import com.xpdustry.nucleus.concurrent.AsyncAwareScheduledTaskHandlerProcessor;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.database.PostgresDatabase;
import com.xpdustry.nucleus.database.PostgresDatabaseImpl;
import com.xpdustry.nucleus.gatekeeper.GatekeeperController;
import com.xpdustry.nucleus.gatekeeper.GatekeeperPipeline;
import com.xpdustry.nucleus.message.MessagePublisher;
import com.xpdustry.nucleus.metric.MetricCollector;
import com.xpdustry.nucleus.metric.MetricRegistry;
import com.xpdustry.nucleus.metric.MindustryMetricCollector;
import com.xpdustry.nucleus.metric.PostgresMetricRegistry;
import com.xpdustry.nucleus.network.InetAddressInfoProvider;
import com.xpdustry.nucleus.network.InetAddressWhitelist;
import com.xpdustry.nucleus.text.BadWordFinder;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO what a mess...
public final class NucleusPlugin extends BaseMindustryPlugin {

    private final ConfigManager config =
            this.addListener(new ConfigManager(this.directory().resolve("config.properties")));

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
            .name("nucleus-worker-", 0)
            .uncaughtExceptionHandler(
                    (thread, e) -> logger().error("An uncaught exception occurred in thread {}", thread.getName(), e))
            .factory());

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final PostgresDatabase database = this.addListener(
            new PostgresDatabaseImpl(this.config, this.directory().resolve("postgres")));

    private final MessagePublisher network =
            this.addListener(new MessagePublisher(this.config, this.executor, this.database));

    private final BadWordFinder badWords = this.addListener(new BadWordFinder(this.gson));

    private final InetAddressInfoProvider addressInfoProvider =
            this.addListener(new InetAddressInfoProvider(this.config, this.gson, this.httpClient, this.database));
    private final InetAddressWhitelist addressWhitelist = new InetAddressWhitelist(this.database);

    private final GatekeeperPipeline gatekeeperPipeline = new GatekeeperPipeline();

    private final MetricRegistry metrics =
            this.addListener(new PostgresMetricRegistry(this.config, this.executor, this.gson, this.database));

    @Override
    public void onInit() {
        final var processors = PluginAnnotationProcessor.compose(
                new AsyncAwareScheduledTaskHandlerProcessor(this, this.executor),
                PluginAnnotationProcessor.events(this),
                PluginAnnotationProcessor.triggers(this),
                PluginAnnotationProcessor.playerActions(this));
        this.metrics.register(MetricCollector.withMainThread(this.addListener(new MindustryMetricCollector())));
        this.addListener(new GatekeeperController(
                this.config,
                this.executor,
                this.gatekeeperPipeline,
                this.badWords,
                this.addressInfoProvider,
                this.addressWhitelist));
        // TODO Add a saner way to dynamically adding listeners
        final var listeners = this.listeners();
        for (int i = 0; i < listeners.size(); i++) {
            final var results = processors.process(listeners.get(i));
            final var tasks = results.orElse(List.of()).stream()
                    .filter(o -> o instanceof MindustryTask)
                    .map(MindustryTask.class::cast)
                    .toList();
            if (!tasks.isEmpty()) {
                this.addListener(new PluginListener() {
                    @Override
                    public void onExit() {
                        tasks.forEach(MindustryTask::cancel);
                    }
                });
                i++;
            }
        }
    }

    @Override
    public void onExit() {
        this.executor.close();
    }
}
