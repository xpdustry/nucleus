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
import fr.xpdustry.nucleus.core.message.JavelinMessenger;
import fr.xpdustry.nucleus.discord.commands.EchoCommand;
import fr.xpdustry.nucleus.discord.commands.JavelinCommand;
import fr.xpdustry.nucleus.discord.commands.PingCommand;
import fr.xpdustry.nucleus.discord.interaction.SlashCommandManager;
import fr.xpdustry.nucleus.discord.service.AutoUpdateService;
import fr.xpdustry.nucleus.discord.service.BridgeService;
import fr.xpdustry.nucleus.discord.service.ReportService;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.aeonbits.owner.ConfigFactory;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NucleusBotLauncher {

    private static final Logger logger = LoggerFactory.getLogger(NucleusBotLauncher.class);

    private NucleusBotLauncher() {}

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

        logger.info("Nucleus is initialized!");
        final var bot = new NucleusBot(configuration, discord, messenger);

        logger.info("Registering services...");
        bot.addService(new BridgeService(bot));
        bot.addService(new AutoUpdateService(bot));
        bot.addService(new ReportService(bot));

        logger.info("Registering commands...");
        final var registry = new SlashCommandManager(discord);
        registry.register(new EchoCommand());
        registry.register(new JavelinCommand(authenticator));
        registry.register(new PingCommand());
        registry.compile().join();

        logger.info("Nucleus Bot is ready!");
    }
}
