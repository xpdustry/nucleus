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

import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.core.event.BanBroadcastEvent;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.net.Packets.KickReason;

public final class BanBroadcastService implements PluginListener {

    private final NucleusPlugin nucleus;

    public BanBroadcastService(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginLoad() {
        // TODO Add random messages
        this.nucleus.getMessenger().subscribe(BanBroadcastEvent.class, event -> {
            if (event.type() == BanBroadcastEvent.Type.BAN) {
                Vars.netServer.admins.banPlayerID(event.targetUuid());
                Groups.player.each(player -> player.uuid().equals(event.targetUuid()), player -> {
                    player.kick(KickReason.banned);
                    Call.sendMessage(
                            "[scarlet]" + player.plainName() + " has been thanos snapped by " + event.author());
                });
            } else {
                Vars.netServer.admins.handleKicked(event.targetUuid(), event.targetIp(), 30 * 60 * 1000);
                Groups.player.each(player -> player.uuid().equals(event.targetUuid()), player -> {
                    player.kick(KickReason.kick);
                    Call.sendMessage("[scarlet]" + player.plainName() + " has been kicked by " + event.author());
                });
            }
        });
    }
}
