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
package fr.xpdustry.nucleus.discord;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.javelin.UserAuthenticator;
import fr.xpdustry.nucleus.common.annotation.NucleusExecutor;
import fr.xpdustry.nucleus.common.application.NucleusApplication;
import fr.xpdustry.nucleus.common.configuration.ConfigurationFactory;
import fr.xpdustry.nucleus.common.message.JavelinMessageService;
import fr.xpdustry.nucleus.common.message.MessageService;
import fr.xpdustry.nucleus.common.network.DiscoveryService;
import fr.xpdustry.nucleus.discord.configuration.NucleusDiscordConfiguration;
import fr.xpdustry.nucleus.discord.interaction.InteractionManager;
import fr.xpdustry.nucleus.discord.interaction.SimpleInteractionManager;
import fr.xpdustry.nucleus.discord.network.VersionControlDiscoveryService;
import fr.xpdustry.nucleus.discord.service.DiscordService;
import fr.xpdustry.nucleus.discord.service.SimpleDiscordService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NucleusDiscordModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DiscordService.class).to(SimpleDiscordService.class).in(Singleton.class);
        bind(InteractionManager.class).to(SimpleInteractionManager.class).in(Singleton.class);
        bind(Executor.class).annotatedWith(NucleusExecutor.class).toInstance(Executors.newCachedThreadPool());
        bind(DiscoveryService.class).to(VersionControlDiscoveryService.class).in(Singleton.class);
        // TODO bindListener does not work... WHY?
        bind(Logger.class).toProvider(() -> LoggerFactory.getLogger("Nucleus"));
    }

    @Provides
    @Singleton
    UserAuthenticator provideUserAuthenticator(final NucleusApplication application) {
        return UserAuthenticator.create(application.getDataDirectory().resolve("users.bin.gz"));
    }

    @Provides
    @Singleton
    MessageService provideMessageService(
            final NucleusDiscordConfiguration configuration, final UserAuthenticator authenticator) {
        return new JavelinMessageService(JavelinSocket.server(
                configuration.getJavelinPort(), configuration.getJavelinWorkers(), true, authenticator));
    }

    @Provides
    @Singleton
    public NucleusDiscordConfiguration provideConfiguration(final ConfigurationFactory factory) {
        return factory.create(NucleusDiscordConfiguration.class);
    }
}
