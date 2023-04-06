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

import fr.xpdustry.nucleus.core.NucleusApplication;
import fr.xpdustry.nucleus.core.message.Messenger;
import fr.xpdustry.nucleus.core.translation.Translator;
import fr.xpdustry.nucleus.core.util.NucleusPlatform;
import fr.xpdustry.nucleus.core.util.NucleusVersion;
import fr.xpdustry.nucleus.discord.service.NucleusDiscordService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;

public final class NucleusBot implements NucleusApplication {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NucleusBot.class);

    private final NucleusBotConfiguration configuration;
    private final DiscordApi discordApi;
    private final Messenger messenger;
    private final Translator translator;
    private final List<NucleusDiscordService> services = new ArrayList<>();

    public NucleusBot(
            final NucleusBotConfiguration configuration,
            final DiscordApi discordApi,
            final Messenger messenger,
            final Translator translator) {
        this.configuration = configuration;
        this.discordApi = discordApi;
        this.messenger = messenger;
        this.translator = translator;
    }

    @Override
    public NucleusBotConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public DiscordApi getDiscordApi() {
        return discordApi;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public Translator getTranslator() {
        return translator;
    }

    public void addService(final NucleusDiscordService service) {
        services.add(service);
        service.onNucleusDiscordInit();
    }

    @Override
    public NucleusVersion getVersion() {
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

    @Override
    public NucleusPlatform getPlatform() {
        return NucleusPlatform.DISCORD;
    }

    public Server getMainServer() {
        return discordApi.getServers().iterator().next();
    }

    public TextChannel getSystemChannel() {
        return getMainServer()
                .getTextChannelById(configuration.getSystemChannel())
                .orElseThrow();
    }

    public TextChannel getReportChannel() {
        return getMainServer()
                .getTextChannelById(configuration.getReportChannel())
                .orElseThrow();
    }

    public void shutdown() {
        logger.info("Shutting down the bot...");
        services.forEach(NucleusDiscordService::onNucleusDiscordExit);
        discordApi.disconnect();
        messenger.close();
        logger.info("Bot has been shut down.");
    }

    public void restart() {
        shutdown();
        System.exit(2);
    }
}
