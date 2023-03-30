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
package fr.xpdustry.nucleus.mindustry.service;

import arc.util.Strings;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.util.Players;
import fr.xpdustry.nucleus.core.event.ImmutablePlayerActionEvent;
import fr.xpdustry.nucleus.core.event.PlayerActionEvent;
import fr.xpdustry.nucleus.core.util.NucleusPlatform;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.util.Locale;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.gen.Player;

public final class DiscordBridgeService implements PluginListener {

    private final NucleusPlugin nucleus;

    public DiscordBridgeService(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginLoad() {
        this.nucleus.getMessenger().subscribe(PlayerActionEvent.class, event -> {
            if (event.getPlatform() == NucleusPlatform.DISCORD
                    && this.nucleus.getConfiguration().getServerName().equals(event.getServerName())
                    && event.getType() == PlayerActionEvent.Type.CHAT) {
                Call.sendMessage("[coral][[[white]" + Iconc.discord + "[]][[[orange]" + event.getPlayerName()
                        + "[coral]]:[white] " + event.getPayload().orElseThrow());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(final EventType.PlayerJoin event) {
        this.nucleus
                .getMessenger()
                .send(ImmutablePlayerActionEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.nucleus.getConfiguration().getServerName())
                        .platform(NucleusPlatform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.JOIN)
                        .build());
    }

    @EventHandler
    public void onPlayerChat(final EventType.PlayerChatEvent event) {
        if (event.message.startsWith("/")) {
            return;
        }
        final var rawMessage = Strings.stripColors(event.message);
        final var locale = Players.getLocale(event.player);

        if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            this.sendChatMessage(event.player, rawMessage);
            return;
        }
        this.nucleus
                .getTranslator()
                .translate(rawMessage, locale, Locale.ENGLISH)
                .thenApply(translation ->
                        rawMessage.equalsIgnoreCase(translation) ? rawMessage : rawMessage + " (" + translation + ")")
                .exceptionally(throwable -> rawMessage)
                .thenAccept(message -> this.sendChatMessage(event.player, message));
    }

    private void sendChatMessage(final Player player, final String message) {
        this.nucleus
                .getMessenger()
                .send(ImmutablePlayerActionEvent.builder()
                        .playerName(player.plainName())
                        .serverName(this.nucleus.getConfiguration().getServerName())
                        .platform(NucleusPlatform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.CHAT)
                        .payload(message)
                        .build());
    }

    @EventHandler
    public void onPlayerLeave(final EventType.PlayerLeave event) {
        this.nucleus
                .getMessenger()
                .send(ImmutablePlayerActionEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.nucleus.getConfiguration().getServerName())
                        .platform(NucleusPlatform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.QUIT)
                        .build());
    }
}
