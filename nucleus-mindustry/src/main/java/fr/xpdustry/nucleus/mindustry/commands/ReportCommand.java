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
package fr.xpdustry.nucleus.mindustry.commands;

import arc.struct.Seq;
import arc.util.CommandHandler;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.argument.PlayerArgument;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.util.ArcCollections;
import fr.xpdustry.nucleus.core.event.ImmutablePlayerReportEvent;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.ListTransformer;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuOption;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.StateKey;
import fr.xpdustry.nucleus.mindustry.util.PlayerMap;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public final class ReportCommand implements PluginListener {

    private static final StateKey<Player> REPORTED_PLAYER = StateKey.of("reported_player", Player.class);

    // TODO Fix exploit where a player that leaves and rejoins can bypass the cooldown
    private final PlayerMap<Long> cooldown = PlayerMap.create();
    private final NucleusPlugin nucleus;
    private final MenuInterface playerMenu;
    private final MenuInterface reportMenu;

    public ReportCommand(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;

        this.reportMenu = MenuInterface.create(nucleus).addTransformer((view, pane) -> pane.setTitle("Report a player")
                .setContent(
                        """
                    Select a reason to report [accent]%s[].
                    [red]WARNING[]: Using this command without a valid reason will result in a warn.
                    """
                                .formatted(view.getState()
                                        .get(REPORTED_PLAYER)
                                        .orElseThrow()
                                        .plainName()))
                .addOptionRow(reportReason("Griefing"))
                .addOptionRow(reportReason("Spamming"))
                .addOptionRow(reportReason("Toxicity"))
                .addOptionRow(reportReason("Sabotage"))
                .addOptionRow(MenuOption.of("[darkgray]Cancel", Action.back())));

        this.playerMenu = MenuInterface.create(nucleus)
                .addTransformer((view, pane) -> pane.setTitle("Select a player to report"))
                .addTransformer(new ListTransformer<Player>()
                        .setElementProvider(() -> ArcCollections.immutableList(Groups.player.copy(new Seq<>())))
                        .setElementRenderer(Player::plainName)
                        .setChoiceAction((view, player) ->
                                this.reportMenu.open(view, state -> state.set(REPORTED_PLAYER, player))));
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = nucleus.getClientCommands();

        handler.removeCommand("vote");
        manager.command(manager.commandBuilder("votekick")
                .meta(CommandMeta.DESCRIPTION, "Votekick a player (this command is disabled).")
                .argument(PlayerArgument.of("player"))
                .handler(ctx -> this.reportMenu.open(
                        ctx.getSender().getPlayer(), state -> state.set(REPORTED_PLAYER, ctx.get("player")))));

        manager.command(manager.commandBuilder("report")
                .meta(CommandMeta.DESCRIPTION, "Report a player.")
                .handler(ctx -> this.playerMenu.open(ctx.getSender().getPlayer())));
    }

    private void report(final Player sender, final Player target, final String reason) {
        if (!sender.admin() && this.cooldown.get(sender, 0L) - System.currentTimeMillis() > 0) {
            sender.sendMessage("[red]Ayo, chill the fuck up, wait a minute before reporting someone again.");
            return;
        }

        if (sender.uuid().equals(target.uuid())) {
            sender.sendMessage("[red]You can't report yourself >:(");
            return;
        }

        if (!this.nucleus.getMessenger().isOpen()) {
            sender.sendMessage("[red]The report system is down, please contact an administrator.");
            return;
        }

        // One minute cooldown
        this.cooldown.set(sender, System.currentTimeMillis() + (60 * 1000L));
        this.nucleus
                .getMessenger()
                .send(ImmutablePlayerReportEvent.builder()
                        .playerName(sender.plainName())
                        .serverName(this.nucleus.getConfiguration().getServerName())
                        .reportedPlayerName(target.plainName())
                        .reportedPlayerIp(target.ip())
                        .reportedPlayerUuid(target.uuid())
                        .reason(reason)
                        .build());

        Groups.player.each(p -> p.sendMessage("[scarlet]" + target.plainName() + " has been reported for '"
                + reason + "' by "
                + sender.plainName() + "."));
    }

    private MenuOption reportReason(final String reason) {
        return MenuOption.of(
                reason,
                Action.closeAll()
                        .then(view -> report(
                                view.getViewer(),
                                view.getState().get(REPORTED_PLAYER).orElseThrow(),
                                reason)));
    }
}
