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
package fr.xpdustry.nucleus.discord.listeners;

import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.nucleus.api.event.ImmutablePlayerActionEvent;
import fr.xpdustry.nucleus.api.event.PlayerActionEvent;
import fr.xpdustry.nucleus.api.event.PlayerEvent;
import fr.xpdustry.nucleus.api.util.Platform;
import fr.xpdustry.nucleus.discord.NucleusBotConfiguration;
import javax.annotation.PostConstruct;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.stereotype.Component;

@Component
public final class BridgeListener implements MessageCreateListener {

    private final NucleusBotConfiguration configuration;
    private final DiscordApi api;
    private final JavelinSocket socket;

    public BridgeListener(
            final NucleusBotConfiguration configuration, final DiscordApi api, final JavelinSocket socket) {
        this.configuration = configuration;
        this.api = api;
        this.socket = socket;
    }

    @PostConstruct
    public void init() {
        this.socket.subscribe(PlayerActionEvent.class, event -> {
            final var builder = new MessageBuilder()
                    .setAllowedMentions(new AllowedMentionsBuilder()
                            .setMentionEveryoneAndHere(false)
                            .setMentionRoles(false)
                            .setMentionUsers(false)
                            .build());
            switch (event.getType()) {
                case JOIN -> builder.append(":arrow_right: **")
                        .append(event.getPlayerName())
                        .append("** has joined the server.");
                case QUIT -> builder.append(":arrow_left: **")
                        .append(event.getPlayerName())
                        .append("** has left the server.");
                case CHAT -> builder.append(":arrow_forward: **")
                        .append(event.getPlayerName())
                        .append("**: ")
                        .append(event.getPayload().orElseThrow());
            }
            builder.send(getLinkedServerChannel(event));
        });
    }

    @Override
    public void onMessageCreate(final MessageCreateEvent event) {
        if (event.getMessageAuthor().isYourself()
                || event.getServerTextChannel().isEmpty()) {
            return;
        }
        final var channel = event.getServerTextChannel().orElseThrow();
        channel.getCategory().ifPresent(category -> {
            if (category.getId() == configuration.getChannels().getServers()) {
                socket.sendEvent(ImmutablePlayerActionEvent.builder()
                        .playerName(event.getMessageAuthor().getDisplayName().replace("[", "[["))
                        .serverName(channel.getName())
                        .platform(Platform.DISCORD)
                        .type(PlayerActionEvent.Type.CHAT)
                        .payload(event.getMessageContent())
                        .build());
            }
        });
    }

    private TextChannel getLinkedServerChannel(final PlayerEvent event) {
        final var server = this.api.getServers().iterator().next();
        final var category = server.getChannelCategoryById(
                        configuration.getChannels().getServers())
                .orElseThrow();
        return category.getChannels().stream()
                .filter(c -> c.getName().equals(event.getServerName()))
                .findFirst()
                .orElseGet(() -> server.createTextChannelBuilder()
                        .setCategory(category)
                        .setName(event.getServerName())
                        .create()
                        .join())
                .asTextChannel()
                .orElseThrow();
    }
}
