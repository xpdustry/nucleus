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
import fr.xpdustry.nucleus.api.application.NucleusPlatform;
import fr.xpdustry.nucleus.api.application.NucleusRuntime;
import fr.xpdustry.nucleus.api.application.NucleusVersion;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.common.configuration.ConfigurationFactory;
import fr.xpdustry.nucleus.common.message.JavelinMessageService;
import fr.xpdustry.nucleus.discord.configuration.NucleusDiscordConfiguration;
import fr.xpdustry.nucleus.discord.interaction.InteractionManager;
import fr.xpdustry.nucleus.discord.interaction.SimpleInteractionManager;
import fr.xpdustry.nucleus.discord.service.DiscordService;
import fr.xpdustry.nucleus.discord.service.SimpleDiscordService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NucleusDiscordModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DiscordService.class).to(SimpleDiscordService.class).in(Singleton.class);
        bind(InteractionManager.class).to(SimpleInteractionManager.class).in(Singleton.class);
        // TODO bindListener does not work... WHY?
        bind(Logger.class).toProvider(() -> LoggerFactory.getLogger("Nucleus"));
    }

    @Provides
    @Singleton
    NucleusRuntime provideRuntime() {
        return NucleusRuntime.builder()
                .setAsyncExecutor(Executors.newCachedThreadPool())
                .setPlatform(NucleusPlatform.DISCORD)
                .setVersion(getVersion())
                .setApplicationJar(getApplicationJarLocation())
                .setDataDirectory(Path.of("."))
                .build();
    }

    @Provides
    @Singleton
    UserAuthenticator provideUserAuthenticator(final NucleusRuntime runtime) {
        return UserAuthenticator.create(runtime.getDataDirectory().resolve("users.bin.gz"));
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

    private Path getApplicationJarLocation() {
        final var codeSource = getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Could not find code source");
        }
        final var location = codeSource.getLocation();
        if (location == null) {
            throw new IllegalStateException("Could not find code source location");
        }
        return Path.of(location.getPath());
    }

    private NucleusVersion getVersion() {
        final var stream = getClass().getClassLoader().getResourceAsStream("VERSION.txt");
        if (stream == null) {
            throw new IllegalStateException("Could not find VERSION.txt, using default version");
        }
        try (final var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return NucleusVersion.parse(reader.readLine());
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load version", e);
        }
    }
}
