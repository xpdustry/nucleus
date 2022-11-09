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

import fr.xpdustry.distributor.api.event.EventBusListener;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.nucleus.common.event.ImmutablePlayerActionEvent;
import fr.xpdustry.nucleus.common.event.PlayerActionEvent;
import fr.xpdustry.nucleus.common.util.Platform;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.net.Administration;

public final class DiscordBridge implements EventBusListener {

    private final Administration.Config serverName;

    public DiscordBridge(final Administration.Config serverName) {
        this.serverName = serverName;
        JavelinPlugin.getJavelinSocket().subscribe(PlayerActionEvent.class, event -> {
            if (event.getPlatform() == Platform.DISCORD
                    && serverName.string().equals(event.getServerName())
                    && event.getType() == PlayerActionEvent.Type.CHAT) {
                Call.sendMessage("[coral][[[white]" + Iconc.discord + "[]][[[orange]" + event.getPlayerName()
                        + "[coral]]:[white] " + event.getPayload().orElseThrow());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(final EventType.PlayerJoin event) {
        JavelinPlugin.getJavelinSocket()
                .sendEvent(ImmutablePlayerActionEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.serverName.string())
                        .platform(Platform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.JOIN)
                        .build());
    }

    @EventHandler
    public void onPlayerMessage(final EventType.PlayerChatEvent event) {
        if (event.message.startsWith("/")) {
            return;
        }
        JavelinPlugin.getJavelinSocket()
                .sendEvent(ImmutablePlayerActionEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.serverName.string())
                        .platform(Platform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.CHAT)
                        .payload(event.message)
                        .build());
    }

    @EventHandler
    public void onPlayerQuit(final EventType.PlayerLeave event) {
        JavelinPlugin.getJavelinSocket()
                .sendEvent(ImmutablePlayerActionEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.serverName.string())
                        .platform(Platform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.QUIT)
                        .build());
    }
}
