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

import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.core.event.ImmutablePlayerActionEvent;
import fr.xpdustry.nucleus.core.event.PlayerActionEvent;
import fr.xpdustry.nucleus.core.util.NucleusPlatform;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Iconc;

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
        this.nucleus
                .getMessenger()
                .send(ImmutablePlayerActionEvent.builder()
                        .playerName(event.player.plainName())
                        .serverName(this.nucleus.getConfiguration().getServerName())
                        .platform(NucleusPlatform.MINDUSTRY)
                        .type(PlayerActionEvent.Type.CHAT)
                        .payload(event.message)
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
