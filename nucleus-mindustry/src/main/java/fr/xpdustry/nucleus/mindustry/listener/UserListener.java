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

import cloud.commandframework.meta.CommandMeta;
import com.google.common.net.InetAddresses;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.database.DatabaseService;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.annotation.ServerSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Player;

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
                .handler(ctx -> this.clientCommandManager
                        .recipe(ctx)
                        .thenApplyAsync(result -> this.databaseService
                                .getUserManager()
                                .findByIdOrCreate(result.getSender().getPlayer().uuid())
                                .join()
                                .getPlayTime()
                                .plus(getSessionPlayTime(result.getSender().getPlayer())))
                        // TODO Format this god awful mess
                        .thenAccept(duration -> ctx.getSender()
                                .sendMessage("Your play time is "
                                        + duration.toHoursPart() + " hours, "
                                        + duration.toMinutesPart() + " minutes and "
                                        + duration.toSecondsPart() + " seconds."))
                        .execute()));
    }

    @EventHandler
    public void onPlayerConnectionConfirmed(final EventType.PlayerConnectionConfirmed event) {
        playtime.put(event.player.uuid(), System.currentTimeMillis());

        final var address = InetAddresses.forString(event.player.ip());
        databaseService.getUserManager().updateOrCreate(event.player.uuid(), user -> user.setLastName(
                        event.player.plainName())
                .addName(event.player.plainName())
                .setLastAddress(address)
                .addAddress(address)
                .setTimesJoined(user.getTimesJoined() + 1));
    }

    @EventHandler
    public void onGameOver(final EventType.GameOverEvent event) {
        Groups.player.forEach(player -> this.databaseService
                .getUserManager()
                .updateOrCreate(player.uuid(), user -> user.setGamesPlayed(user.getGamesPlayed() + 1)));
    }

    @EventHandler
    public void onPlayerLeave(final EventType.PlayerLeave event) {
        // TODO Create an helper class to capture completable future exceptions
        this.databaseService
                .getUserManager()
                .updateOrCreate(
                        event.player.uuid(),
                        user -> user.setPlayTime(user.getPlayTime().plus(getSessionPlayTime(event.player))))
                .whenComplete((empty, throwable) -> playtime.remove(event.player.uuid()));
    }

    @SuppressWarnings("NullAway") // The static analyzer thinks players returns nullable values, but it doesn't
    private Duration getSessionPlayTime(final Player player) {
        return Duration.ofMillis(System.currentTimeMillis() - playtime.get(player.uuid()));
    }
}
