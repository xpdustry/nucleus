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

import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.javelin.UserAuthenticator;
import fr.xpdustry.nucleus.common.message.JavelinMessenger;
import fr.xpdustry.nucleus.discord.commands.EchoCommand;
import fr.xpdustry.nucleus.discord.commands.JavelinCommand;
import fr.xpdustry.nucleus.discord.commands.PingCommand;
import fr.xpdustry.nucleus.discord.commands.ShutdownCommand;
import fr.xpdustry.nucleus.discord.interaction.SlashCommandManager;
import fr.xpdustry.nucleus.discord.service.NucleusBotService;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import org.aeonbits.owner.ConfigFactory;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NucleusBotLauncher {

    private static final Logger logger = LoggerFactory.getLogger(NucleusBotLauncher.class);

    public static void main(final String[] args) {
        logger.info("Hello world!");
        final var configuration = ConfigFactory.create(NucleusBotConfiguration.class);

        logger.info("Connecting to the Discord API...");
        final var discord = new DiscordApiBuilder()
                .setToken(configuration.getToken())
                .setUserCacheEnabled(true)
                .addIntents(
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.DIRECT_MESSAGES)
                .login()
                .orTimeout(15L, TimeUnit.SECONDS)
                .join();

        logger.info("Starting the Javelin Server...");
        final var authenticator = UserAuthenticator.create(Path.of(".", "users.bin.gz"));
        final var socket = JavelinSocket.server(
                configuration.getJavelinPort(), configuration.getJavelinWorkers(), true, authenticator);
        final var messenger = new JavelinMessenger(socket, 10);
        messenger.start();

        final var bot = new NucleusBot(configuration, discord, messenger, authenticator);

        logger.info("Registering commands...");
        final var registry = new SlashCommandManager(bot.getDiscordApi());
        registry.register(new EchoCommand());
        registry.register(new PingCommand());
        registry.register(new JavelinCommand(bot.getAuthenticator()));
        registry.register(new ShutdownCommand(bot));
        registry.compile().join();

        logger.info("Registering services...");
        ServiceLoader.load(NucleusBotService.class).forEach(service -> {
            logger.info("> Service: {}", service.getClass().getName());
            service.onNucleusBotReady(bot);
        });

        logger.info("Nucleus Bot is ready!");
    }
}
