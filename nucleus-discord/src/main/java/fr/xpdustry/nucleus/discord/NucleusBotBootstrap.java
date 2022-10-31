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

import fr.xpdustry.nucleus.common.NucleusApplicationProvider;
import fr.xpdustry.nucleus.discord.commands.StandardCommands;
import org.aeonbits.owner.ConfigFactory;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class NucleusBotBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(NucleusBotBootstrap.class);

    public static void main(final String[] args) {
        logger.info("Hello world :) Starting Nucleus...");

        final var config = ConfigFactory.create(NucleusBotConfig.class);
        if (config.getToken().isBlank()) {
            throw new RuntimeException("The bot token is not set.");
        }
        if (config.getCommandPrefix().isBlank()) {
            throw new RuntimeException("The command prefix is not set.");
        }

        final DiscordApi api;

        try {
            api = new DiscordApiBuilder()
                    .setToken(config.getToken())
                    .setUserCacheEnabled(true)
                    .addIntents(
                            Intent.MESSAGE_CONTENT,
                            Intent.GUILDS,
                            Intent.GUILD_MEMBERS,
                            Intent.GUILD_MESSAGES,
                            Intent.GUILD_MESSAGE_REACTIONS,
                            Intent.DIRECT_MESSAGES)
                    .login()
                    .get(10L, TimeUnit.SECONDS);
        } catch (final InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to create the api instance.", e);
        }

        final var bot = new NucleusBot(config, api);
        NucleusApplicationProvider.set(bot);
        logger.info("Successfully started Nucleus, beginning initialization.");

        System.out.println(bot.getAnnotationParser().parse(new StandardCommands(bot)));
        logger.info("Successfully initialized Nucleus.");
    }
}
