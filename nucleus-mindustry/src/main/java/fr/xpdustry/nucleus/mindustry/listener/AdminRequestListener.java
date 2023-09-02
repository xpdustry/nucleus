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
package fr.xpdustry.nucleus.mindustry.listener;

import arc.Events;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.database.model.Punishment.Kind;
import fr.xpdustry.nucleus.mindustry.moderation.ModerationService;
import fr.xpdustry.nucleus.mindustry.testing.ui.input.TextInputInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.StateKey;
import java.util.Locale;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.AdminRequestCallPacket;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.net.Administration.TraceInfo;
import org.slf4j.Logger;

public final class AdminRequestListener implements NucleusListener {

    private static final StateKey<PunishmentData> PUNISHMENT_DATA = StateKey.of("punishment", PunishmentData.class);

    private final TextInputInterface reasonInput;

    @Inject
    private Logger logger;

    @Inject
    public AdminRequestListener(final MindustryPlugin plugin, final ModerationService moderation) {
        this.reasonInput = TextInputInterface.create(plugin)
                .setMaxInputLength(64)
                .addTransformer((view, pane) -> pane.setTitle("The reason of the "
                        + view.getState()
                                .get(PUNISHMENT_DATA)
                                .orElseThrow()
                                .kind
                                .name()
                                .toLowerCase(Locale.ROOT)))
                .setInputAction((view, reason) -> {
                    view.close();
                    final var data = view.getState().get(PUNISHMENT_DATA).orElseThrow();
                    moderation
                            .punish(view.getViewer(), data.target, data.kind, reason)
                            .exceptionally(throwable -> {
                                logger.error(
                                        "Failed to punish {} ({})",
                                        data.target.plainName(),
                                        data.target.uuid(),
                                        throwable);
                                return null;
                            });
                });
    }

    @Override
    public void onNucleusInit() {
        Vars.net.handleServer(AdminRequestCallPacket.class, (con, packet) -> {
            if (con.player == null) {
                logger.warn(
                        "Received admin request from non-existent player (uuid: {}, ip: {})", con.uuid, con.address);
                return;
            }

            if (!con.player.admin()) {
                logger.warn(
                        "{} ({}) attempted to perform an admin action without permission",
                        con.player.plainName(),
                        con.player.uuid());
                return;
            }

            if (packet.other == null) {
                logger.warn(
                        "{} ({}) attempted to perform an admin action on non-existent",
                        con.player.plainName(),
                        con.player.uuid());
                return;
            }

            if (packet.other.admin()) {
                logger.warn(
                        "{} ({}) attempted to perform an admin action on the admin {} ({})",
                        con.player.plainName(),
                        con.player.uuid(),
                        packet.other.plainName(),
                        packet.other.uuid());
                return;
            }

            Events.fire(new EventType.AdminRequestEvent(con.player, packet.other, packet.action));

            switch (packet.action) {
                case wave -> {
                    Vars.logic.skipWave();
                    logger.info("{} ({}) has skipped the wave", con.player.plainName(), con.player.uuid());
                }
                case trace -> {
                    final var stats = Vars.netServer.admins.getInfo(packet.other.uuid());
                    final var info = new TraceInfo(
                            packet.other.con.address,
                            packet.other.uuid(),
                            packet.other.con.modclient,
                            packet.other.con.mobile,
                            stats.timesJoined,
                            stats.timesKicked,
                            stats.ips.toArray(),
                            stats.names.toArray());
                    Call.traceInfo(con, packet.other, info);
                    logger.info(
                            "{} ({}) has requested trace info of {} ({})",
                            con.player.plainName(),
                            con.player.uuid(),
                            packet.other.plainName(),
                            packet.other.uuid());
                }
                case ban -> punish(con.player, packet.other, Kind.BAN);
                case kick -> punish(con.player, packet.other, Kind.KICK);
                case switchTeam -> {
                    if (packet.params instanceof Team team) {
                        packet.other.team(team);
                        logger.info(
                                "{} ({}) has switched {} ({}) to team {}",
                                con.player.plainName(),
                                con.player.uuid(),
                                packet.other.plainName(),
                                packet.other.uuid(),
                                team.name);
                    }
                }
                default -> {
                    logger.warn(
                            "{} ({}) attempted to perform an unknown admin action {} on {} ({})",
                            con.player.plainName(),
                            con.player.uuid(),
                            packet.action,
                            packet.other.plainName(),
                            packet.other.uuid());
                }
            }
        });
    }

    private void punish(final Player player, final Player target, final Kind kind) {
        this.reasonInput.open(player, state -> state.set(PUNISHMENT_DATA, new PunishmentData(target, kind)));
    }

    private record PunishmentData(Player target, Kind kind) {}
}
