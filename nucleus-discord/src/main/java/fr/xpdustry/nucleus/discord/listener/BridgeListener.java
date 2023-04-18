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
package fr.xpdustry.nucleus.discord.listener;

import com.vdurmont.emoji.EmojiParser;
import com.vdurmont.emoji.EmojiParser.FitzpatrickAction;
import fr.xpdustry.nucleus.api.application.lifecycle.AutoLifecycleListener;
import fr.xpdustry.nucleus.api.application.NucleusPlatform;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.bridge.PlayerActionMessage;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.discord.NucleusDiscordUtil;
import fr.xpdustry.nucleus.discord.configuration.NucleusDiscordConfiguration;
import fr.xpdustry.nucleus.discord.service.DiscordService;
import javax.inject.Inject;
import org.javacord.api.entity.message.MessageBuilder;

@AutoLifecycleListener
public final class BridgeListener implements LifecycleListener {

    private final NucleusDiscordConfiguration configuration;
    private final DiscordService discordService;
    private final MessageService messageService;

    @Inject
    public BridgeListener(
            final NucleusDiscordConfiguration configuration,
            final DiscordService discordService,
            final MessageService messageService) {
        this.configuration = configuration;
        this.discordService = discordService;
        this.messageService = messageService;
    }

    @Override
    public void onLifecycleInit() {
        this.messageService.subscribe(PlayerActionMessage.class, event -> {
            final var builder = new MessageBuilder().setAllowedMentions(NucleusDiscordUtil.noMentions());
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

            final var server = this.discordService.getMainServer();
            final var category = server.getChannelCategoryById(this.configuration.getServerCategory())
                    .orElseThrow();
            builder.send(category.getChannels().stream()
                    .filter(c -> c.getName().equals(event.getServerIdentifier()))
                    .findFirst()
                    .orElseGet(() -> server.createTextChannelBuilder()
                            .setCategory(category)
                            .setName(event.getServerIdentifier())
                            .create()
                            .join())
                    .asTextChannel()
                    .orElseThrow());
        });

        this.discordService.getDiscordApi().addMessageCreateListener(event -> {
            if (event.getMessageAuthor().isYourself()
                    || event.getServerTextChannel().isEmpty()) {
                return;
            }
            final var channel = event.getServerTextChannel().orElseThrow();
            channel.getCategory().ifPresent(category -> {
                if (category.getId() == this.configuration.getServerCategory()) {
                    this.messageService.publish(PlayerActionMessage.builder()
                            .setPlayerName(event.getMessageAuthor()
                                    .getDiscriminatedName()
                                    .replace("[", "[["))
                            .setServerIdentifier(channel.getName())
                            .setOrigin(NucleusPlatform.DISCORD)
                            .setType(PlayerActionMessage.Type.CHAT)
                            .setMessage(escapeEmojis(event.getMessageContent()))
                            .build());
                }
            });
        });
    }

    private String escapeEmojis(final String message) {
        return EmojiParser.parseToAliases(message, FitzpatrickAction.REMOVE);
    }
}
