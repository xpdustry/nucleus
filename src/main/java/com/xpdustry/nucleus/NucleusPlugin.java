// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus;

import com.google.gson.Gson;
import com.xpdustry.foundation.annotation.PluginAnnotationProcessor;
import com.xpdustry.foundation.plugin.BaseMindustryPlugin;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.database.PostgresDatabase;
import com.xpdustry.nucleus.gatekeeper.GatekeeperController;
import com.xpdustry.nucleus.gatekeeper.GatekeeperPipeline;
import com.xpdustry.nucleus.message.MessagePublisher;
import com.xpdustry.nucleus.metric.MetricExporter;
import com.xpdustry.nucleus.metric.MindustryMetricCollector;
import com.xpdustry.nucleus.network.InetAddressInfoProvider;
import com.xpdustry.nucleus.network.InetAddressWhitelist;
import com.xpdustry.nucleus.text.BadWordFinder;
import java.net.http.HttpClient;

public final class NucleusPlugin extends BaseMindustryPlugin {

    private final PluginAnnotationProcessor<?> processor = PluginAnnotationProcessor.compose(
            PluginAnnotationProcessor.events(this),
            PluginAnnotationProcessor.scheduledTasks(this),
            PluginAnnotationProcessor.playerActions(this));

    private final ConfigManager configManager =
            this.addListener(new ConfigManager(this.directory().resolve("config.properties")));

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final PostgresDatabase database = this.addListener(
            new PostgresDatabase(this.configManager, this.directory().resolve("postgres")));

    private final MessagePublisher network = this.addListener(new MessagePublisher(this.configManager, this.database));

    private final BadWordFinder badWords = this.addListener(new BadWordFinder(this.gson));

    private final InetAddressInfoProvider addressInfoProvider = this.addListener(
            new InetAddressInfoProvider(this.configManager, this.gson, this.httpClient, this.database));
    private final InetAddressWhitelist addressWhitelist = new InetAddressWhitelist(this.database);

    private final GatekeeperPipeline gatekeeperPipeline = new GatekeeperPipeline();

    private final MetricExporter metrics = this.addListener(new MetricExporter(this.configManager, this.httpClient));

    @Override
    public void onInit() {
        this.metrics.register(this.addListener(new MindustryMetricCollector()), false);
        this.addListener(new GatekeeperController(
                this.gatekeeperPipeline,
                this.configManager,
                this.badWords,
                this.addressInfoProvider,
                this.addressWhitelist));
        for (final var listener : this.listeners()) {
            this.processor.process(listener);
        }
    }
}
