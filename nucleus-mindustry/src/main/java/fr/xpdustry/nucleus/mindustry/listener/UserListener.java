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

import arc.struct.ObjectMap;
import arc.util.Strings;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.util.ArcCollections;
import fr.xpdustry.nucleus.api.application.EnableScanning;
import fr.xpdustry.nucleus.api.application.NucleusListener;
import fr.xpdustry.nucleus.api.database.DatabaseService;
import fr.xpdustry.nucleus.api.database.model.User;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.annotation.ServerSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration;
import mindustry.net.Administration.PlayerInfo;

@EnableScanning
public final class UserListener implements NucleusListener {

    private final Map<String, Long> playtime = new HashMap<>();
    private final DatabaseService databaseService;
    private final NucleusPluginCommandManager clientCommandManager;
    private final NucleusPluginCommandManager serverCommandManager;

    @Inject
    public UserListener(
            final DatabaseService databaseService,
            final @ClientSide NucleusPluginCommandManager clientCommandManager,
            final @ServerSide NucleusPluginCommandManager serverCommandManager) {
        this.databaseService = databaseService;
        this.clientCommandManager = clientCommandManager;
        this.serverCommandManager = serverCommandManager;
    }

    @Override
    public void onNucleusInit() {
        this.clientCommandManager.command(this.clientCommandManager
                .commandBuilder("playtime")
                .meta(CommandMeta.DESCRIPTION, "Get your playtime")
                .handler(ctx -> {
                    final var duration = this.databaseService
                            .getUserManager()
                            .findByIdOrCreate(ctx.getSender().getPlayer().uuid())
                            .getPlayTime()
                            .plus(getSessionPlayTime(ctx.getSender().getPlayer()));

                    ctx.getSender()
                            .sendMessage("Your play time is "
                                    + duration.toHoursPart() + " hours, "
                                    + duration.toMinutesPart() + " minutes and "
                                    + duration.toSecondsPart() + " seconds.");
                }));

        this.serverCommandManager.command(this.serverCommandManager
                .commandBuilder("export-user-data")
                .meta(CommandMeta.DESCRIPTION, "Export native user data to the database")
                .handler(ctx -> exportUserData(ctx.getSender())));
    }

    @EventHandler
    public void onPlayerJoin(final EventType.PlayerConnectionConfirmed event) throws UnknownHostException {
        playtime.put(event.player.uuid(), System.currentTimeMillis());

        final var address = InetAddress.getByName(event.player.ip());
        databaseService.getUserManager().updateOrCreate(event.player.uuid(), user -> user.toBuilder()
                .setLastName(event.player.plainName())
                .addName(event.player.plainName())
                .setLastAddress(address)
                .addAddress(address)
                .setTimesJoined(user.getTimesJoined() + 1)
                .build());
    }

    @EventHandler
    public void onGameOver(final EventType.GameOverEvent event) {
        Groups.player.forEach(
                player -> this.databaseService.getUserManager().updateOrCreate(player.uuid(), user -> user.toBuilder()
                        .setGamesPlayed(user.getGamesPlayed() + 1)
                        .build()));
    }

    @EventHandler
    public void onPlayerLeave(final EventType.PlayerLeave event) {
        this.databaseService.getUserManager().updateOrCreate(event.player.uuid(), user -> user.toBuilder()
                .setPlayTime(user.getPlayTime().plus(getSessionPlayTime(event.player)))
                .build());
        playtime.remove(event.player.uuid());
    }

    @SuppressWarnings("unchecked")
    private void exportUserData(final CommandSender sender) {
        final List<PlayerInfo> infos;
        try {
            final var field = Administration.class.getDeclaredField("playerInfo");
            field.setAccessible(true);
            infos = List.copyOf(
                    ArcCollections.immutableMap((ObjectMap<String, PlayerInfo>) field.get(Vars.netServer.admins))
                            .values());
        } catch (final ReflectiveOperationException e) {
            sender.sendWarning("An error occurred while exporting user data: " + e.getMessage());
            return;
        }

        final List<User> users =
                new ArrayList<>((int) this.databaseService.getUserManager().count());
        for (final var info : infos) {
            var user = this.databaseService.getUserManager().findByIdOrCreate(info.id);
            users.add(user.toBuilder()
                    .setLastAddress(
                            user.getLastAddress().isLoopbackAddress()
                                    ? toInetAddress(info.lastIP)
                                    : user.getLastAddress())
                    .setLastName(user.getLastName().equals("<unknown>") ? info.plainLastName() : user.getLastName())
                    .addAllNames(info.names.map(Strings::stripColors))
                    .addAllAddresses(info.ips.map(this::toInetAddress))
                    .setTimesJoined(user.getTimesJoined() + info.timesJoined)
                    .setTimesKicked(user.getTimesKicked() + info.timesKicked)
                    .build());
        }

        this.databaseService.getUserManager().saveAll(users);
        sender.sendMessage("User data exported successfully! (" + infos.size() + " users)");
    }

    @SuppressWarnings("NullAway") // The static analyzer thinks players returns nullable values, but it doesn't
    private Duration getSessionPlayTime(final Player player) {
        return Duration.ofMillis(System.currentTimeMillis() - playtime.get(player.uuid()));
    }

    private InetAddress toInetAddress(final String address) {
        try {
            return InetAddress.getByName(address);
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
