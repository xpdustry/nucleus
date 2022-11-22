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
package fr.xpdustry.nucleus.mindustry.chat;

import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.util.MoreEvents;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.nucleus.common.event.ImmutablePlayerActionEvent;
import fr.xpdustry.nucleus.common.event.PlayerActionEvent;
import fr.xpdustry.nucleus.common.util.Platform;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Iconc;

public final class DiscordBridge implements PluginListener {

    private final NucleusPlugin nucleus;

    public DiscordBridge(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginLoad() {
        JavelinPlugin.getJavelinSocket().subscribe(PlayerActionEvent.class, event -> {
            if (event.getPlatform() == Platform.DISCORD
                    && nucleus.getConfiguration().getServerName().equals(event.getServerName())
                    && event.getType() == PlayerActionEvent.Type.CHAT) {
                Call.sendMessage("[coral][[[white]" + Iconc.discord + "[]][[[orange]" + event.getPlayerName()
                        + "[coral]]:[white] " + event.getPayload().orElseThrow());
            }
        });

        MoreEvents.subscribe(EventType.PlayerJoin.class, event -> JavelinPlugin.getJavelinSocket()
                .sendEvent(ImmutablePlayerActionEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(nucleus.getConfiguration().getServerName())
                        .platform(Platform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.JOIN)
                        .build()));

        MoreEvents.subscribe(EventType.PlayerChatEvent.class, event -> {
            if (event.message.startsWith("/")) {
                return;
            }
            JavelinPlugin.getJavelinSocket()
                    .sendEvent(ImmutablePlayerActionEvent.builder()
                            .playerName(event.player.plainName())
                            .serverName(nucleus.getConfiguration().getServerName())
                            .platform(Platform.MINDUSTRY)
                            .type(PlayerActionEvent.Type.CHAT)
                            .payload(event.message)
                            .build());
        });

        MoreEvents.subscribe(EventType.PlayerLeave.class, event -> JavelinPlugin.getJavelinSocket()
                .sendEvent(ImmutablePlayerActionEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(nucleus.getConfiguration().getServerName())
                        .platform(Platform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.QUIT)
                        .build()));
    }
}
