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
package fr.xpdustry.nucleus.discord.service;

import com.vdurmont.emoji.EmojiParser;
import com.vdurmont.emoji.EmojiParser.FitzpatrickAction;
import fr.xpdustry.nucleus.core.event.ImmutablePlayerActionEvent;
import fr.xpdustry.nucleus.core.event.PlayerActionEvent;
import fr.xpdustry.nucleus.core.util.NucleusPlatform;
import fr.xpdustry.nucleus.discord.NucleusBot;
import fr.xpdustry.nucleus.discord.NucleusBotUtil;
import org.javacord.api.entity.message.MessageBuilder;

public final class BridgeService implements NucleusDiscordService {

    private final NucleusBot bot;

    public BridgeService(final NucleusBot bot) {
        this.bot = bot;
    }

    @Override
    public void onNucleusDiscordInit() {
        bot.getMessenger().subscribe(PlayerActionEvent.class, event -> {
            final var builder = new MessageBuilder().setAllowedMentions(NucleusBotUtil.noMentions());
            switch (event.getType()) {
                case JOIN -> builder.append(":green_square: **")
                        .append(event.getPlayerName())
                        .append("** has joined the server.");
                case QUIT -> builder.append(":red_square: **")
                        .append(event.getPlayerName())
                        .append("** has left the server.");
                case CHAT -> builder.append(":blue_square: **")
                        .append(event.getPlayerName())
                        .append("**: ")
                        .append(event.getMessage().orElseThrow());
            }

            final var server = bot.getDiscordApi().getServers().iterator().next();
            final var category = server.getChannelCategoryById(
                            bot.getConfiguration().getServerCategory())
                    .orElseThrow();
            builder.send(category.getChannels().stream()
                    .filter(c -> c.getName().equals(event.getServerName()))
                    .findFirst()
                    .orElseGet(() -> server.createTextChannelBuilder()
                            .setCategory(category)
                            .setName(event.getServerName())
                            .create()
                            .join())
                    .asTextChannel()
                    .orElseThrow());
        });

        bot.getDiscordApi().addMessageCreateListener(event -> {
            if (event.getMessageAuthor().isYourself()
                    || event.getServerTextChannel().isEmpty()) {
                return;
            }
            final var channel = event.getServerTextChannel().orElseThrow();
            channel.getCategory().ifPresent(category -> {
                if (category.getId() == bot.getConfiguration().getServerCategory()) {
                    bot.getMessenger()
                            .send(ImmutablePlayerActionEvent.builder()
                                    .playerName(event.getMessageAuthor()
                                            .getDiscriminatedName()
                                            .replace("[", "[["))
                                    .serverName(channel.getName())
                                    .platform(NucleusPlatform.DISCORD)
                                    .type(PlayerActionEvent.Type.CHAT)
                                    .message(escapeEmojis(event.getMessageContent()))
                                    .build());
                }
            });
        });
    }

    private String escapeEmojis(final String message) {
        return EmojiParser.parseToAliases(message, FitzpatrickAction.REMOVE);
    }
}
