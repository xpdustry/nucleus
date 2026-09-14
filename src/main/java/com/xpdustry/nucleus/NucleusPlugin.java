// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus;

import com.google.gson.Gson;
import com.xpdustry.foundation.annotation.PluginAnnotationProcessor;
import com.xpdustry.foundation.plugin.BaseMindustryPlugin;
import com.xpdustry.foundation.plugin.PluginFacade;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.foundation.plugin.PluginLogger;
import com.xpdustry.nucleus.concurrent.AsyncAwareScheduledTaskHandlerProcessor;
import com.xpdustry.nucleus.concurrent.ConcurrentModule;
import com.xpdustry.nucleus.config.ConfigModule;
import com.xpdustry.nucleus.database.DatabaseModule;
import com.xpdustry.nucleus.dependency.DependencyModule;
import com.xpdustry.nucleus.dependency.DependencyService;
import com.xpdustry.nucleus.gatekeeper.GatekeeperModule;
import com.xpdustry.nucleus.message.MessageModule;
import com.xpdustry.nucleus.metric.MetricCollectorHandlerProcessor;
import com.xpdustry.nucleus.metric.MetricModule;
import com.xpdustry.nucleus.network.NetworkModule;
import com.xpdustry.nucleus.text.TextModule;
import java.net.http.HttpClient;
import java.nio.file.Path;

public final class NucleusPlugin extends BaseMindustryPlugin {

    private final DependencyService dependencies = DependencyService.create(
            new PluginModule(),
            new ConfigModule(),
            new DatabaseModule(),
            new MessageModule(),
            new TextModule(),
            new NetworkModule(),
            new MetricModule(),
            new GatekeeperModule(),
            new ConcurrentModule());

    @Override
    public void onInit() {
        for (final var dependency : this.dependencies.resolveAll()) {
            if (dependency instanceof PluginListener listener && !(dependency instanceof PluginFacade)) {
                this.addListener(listener);
            }
        }

        final var processors = PluginAnnotationProcessor.compose(
                PluginAnnotationProcessor.events(this),
                PluginAnnotationProcessor.triggers(this),
                PluginAnnotationProcessor.playerActions(this),
                this.dependencies.resolve(AsyncAwareScheduledTaskHandlerProcessor.class),
                this.dependencies.resolve(MetricCollectorHandlerProcessor.class));
        for (final var listener : this.listeners()) {
            processors.process(listener);
        }
    }

    private final class PluginModule implements DependencyModule {

        @Override
        public void configure(final DependencyService.Binder binder) {
            final var directory = NucleusPlugin.this.directory();
            binder.bindInstance(Path.class, "config", directory.resolve("config.properties"));
            binder.bindInstance(Path.class, "postgres", directory.resolve("postgres"));
            binder.bindInstance(PluginLogger.class, NucleusPlugin.this.logger());
            binder.bindInstance(PluginFacade.class, NucleusPlugin.this);
            binder.bindInstance(Gson.class, new Gson());
            binder.bindInstance(HttpClient.class, HttpClient.newHttpClient());
        }
    }
}
