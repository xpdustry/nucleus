/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.mindustry;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.javelin.JavelinConfig.Mode;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.nucleus.api.application.NucleusPlatform;
import fr.xpdustry.nucleus.api.application.NucleusRuntime;
import fr.xpdustry.nucleus.api.application.NucleusVersion;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.api.network.DiscoveryService;
import fr.xpdustry.nucleus.common.configuration.ConfigurationFactory;
import fr.xpdustry.nucleus.common.message.JavelinMessageService;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.annotation.ServerSide;
import fr.xpdustry.nucleus.mindustry.chat.ChatManager;
import fr.xpdustry.nucleus.mindustry.chat.ChatManagerImpl;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.network.BroadcastingDiscoveryService;
import javax.inject.Singleton;
import mindustry.Vars;
import org.slf4j.Logger;

public final class NucleusMindustryModule extends AbstractModule {

    private final NucleusPlugin plugin;

    public NucleusMindustryModule(final NucleusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(ChatManager.class).to(ChatManagerImpl.class).in(Singleton.class);
        bind(MindustryPlugin.class).toInstance(this.plugin);
        bind(Logger.class).toProvider(this.plugin::getLogger);
        bind(DiscoveryService.class).to(BroadcastingDiscoveryService.class).in(Singleton.class);
        bind(NucleusPluginCommandManager.class).annotatedWith(ClientSide.class).toInstance(plugin.clientCommands);
        bind(NucleusPluginCommandManager.class).annotatedWith(ServerSide.class).toInstance(plugin.serverCommands);
    }

    @Provides
    @Singleton
    NucleusRuntime provideRuntime(final NucleusPlugin plugin) {
        return NucleusRuntime.builder()
                .setAsyncExecutor(runnable -> DistributorProvider.get()
                        .getPluginScheduler()
                        .scheduleAsync(plugin)
                        .execute(runnable))
                .setPlatform(NucleusPlatform.MINDUSTRY)
                .setVersion(NucleusVersion.parse(plugin.getDescriptor().getVersion()))
                .setApplicationJar(Vars.mods
                        .getMod(plugin.getDescriptor().getName())
                        .file
                        .file()
                        .toPath())
                .setDataDirectory(plugin.getDirectory())
                .build();
    }

    @Provides
    @Singleton
    NucleusPluginConfiguration provideConfiguration(final ConfigurationFactory factory) {
        return factory.create(NucleusPluginConfiguration.class);
    }

    @Provides
    @Singleton
    MessageService provideMessageService(final Logger logger) {
        if (JavelinPlugin.getJavelinSocket() == JavelinSocket.noop()
                && JavelinPlugin.getJavelinConfig().getMode() != Mode.NONE) {
            throw new IllegalStateException("Javelin is not initialized");
        }
        return new JavelinMessageService(JavelinPlugin.getJavelinSocket()) {
            @Override
            public void onNucleusInit() {
                if (JavelinPlugin.getJavelinConfig().getMode() == Mode.NONE) {
                    logger.warn("Javelin is not enabled!");
                }
            }

            @Override
            public void onNucleusExit() {
                // Managed by JavelinPlugin
            }
        };
    }
}
