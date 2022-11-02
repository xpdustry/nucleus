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
import fr.xpdustry.nucleus.common.event.*;
import fr.xpdustry.nucleus.discord.commands.JavelinCommands;
import fr.xpdustry.nucleus.discord.commands.StandardCommands;
import fr.xpdustry.nucleus.discord.util.DoNotMention;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.aeonbits.owner.ConfigFactory;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.RegularServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NucleusBotLauncher {

    private static final Logger logger = LoggerFactory.getLogger(NucleusBotLauncher.class);

    public static void main(final String[] args) {
        logger.info("Hello world :) Starting Nucleus...");

        final var config = ConfigFactory.create(NucleusBotConfig.class);
        if (config.getToken().isBlank()) {
            throw new RuntimeException("The bot token is not set.");
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

        logger.info("Successfully started Nucleus, beginning initialization.");
        final var bot = new NucleusBot(config, api);
        NucleusApplicationProvider.set(bot);

        logger.info("> Parsing commands");
        bot.getAnnotationParser().parse(new StandardCommands(bot));
        bot.getAnnotationParser().parse(new JavelinCommands(bot.getAuthenticator()));

        logger.info("> Starting the javelin server");
        bot.getSocket().start().orTimeout(10L, TimeUnit.SECONDS).join();

        bot.getSocket().subscribe(PlayerJoinEvent.class, event -> new MessageBuilder()
                .setAllowedMentions(DoNotMention.get())
                .append(":arrow_right: **")
                .append(event.getPlayerName())
                .append("** has joined the server.")
                .send(getLinkedServerChannel(bot, event)));

        bot.getSocket().subscribe(PlayerMessageEvent.class, event -> new MessageBuilder()
                .setAllowedMentions(DoNotMention.get())
                .append(":arrow_forward: **")
                .append(event.getPlayerName())
                .append("**: ")
                .append(event.getMessage())
                .send(getLinkedServerChannel(bot, event)));

        bot.getSocket().subscribe(PlayerQuitEvent.class, event -> new MessageBuilder()
                .setAllowedMentions(DoNotMention.get())
                .append(":arrow_left: **")
                .append(event.getPlayerName())
                .append("** has left the server.")
                .send(getLinkedServerChannel(bot, event)));

        bot
                .getServer()
                .getChannelCategoryById(bot.getConfig().getServerChatCategory())
                .orElseThrow()
                .getChannels()
                .stream()
                .map(RegularServerChannel::asServerTextChannel)
                .map(Optional::orElseThrow)
                .forEach(channel -> channel.addMessageCreateListener(event -> {
                    if (event.getMessageAuthor().isYourself()) {
                        return;
                    }
                    bot.getSocket()
                            .sendEvent(ImmutablePlayerMessageEvent.builder()
                                    .playerName(event.getMessageAuthor()
                                            .getDisplayName()
                                            .replace("[", "[["))
                                    .serverName(event.getServerTextChannel()
                                            .orElseThrow()
                                            .getName())
                                    .message(event.getMessageContent())
                                    .build());
                }));

        logger.info("Successfully initialized Nucleus.");
    }

    private static TextChannel getLinkedServerChannel(final NucleusBot bot, final PlayerEvent event) {
        final var category = bot.getServer()
                .getChannelCategoryById(bot.getConfig().getServerChatCategory())
                .orElseThrow();
        return category.getChannels().stream()
                .filter(c -> c.getName().equals(event.getServerName()))
                .findFirst()
                .orElseGet(() -> bot.getServer()
                        .createTextChannelBuilder()
                        .setCategory(category)
                        .setName(event.getServerName())
                        .create()
                        .join())
                .asTextChannel()
                .orElseThrow();
    }
}
