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

import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.argument.PlayerArgument;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.api.annotation.NucleusAutoListener;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.api.moderation.PlayerReportMessage;
import fr.xpdustry.nucleus.mindustry.NucleusPluginConfiguration;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuOption;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.PaginatedMenuInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.StateKey;
import fr.xpdustry.nucleus.mindustry.util.PlayerMap;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Player;

@NucleusAutoListener
public final class ReportCommand implements LifecycleListener {

    private static final StateKey<Player> REPORTED_PLAYER = StateKey.of("reported_player", Player.class);

    // TODO Fix exploit where a player that leaves and rejoins can bypass the cooldown
    private final PlayerMap<Long> cooldown = PlayerMap.create();
    private final PaginatedMenuInterface<Player> playerMenu;
    private final MenuInterface reportMenu;

    private final MindustryPlugin plugin;
    private final NucleusPluginConfiguration configuration;
    private final NucleusPluginCommandManager clientCommandManager;
    private final MessageService messageService;

    @Inject
    public ReportCommand(
            final MindustryPlugin plugin,
            final NucleusPluginConfiguration configuration,
            final @ClientSide NucleusPluginCommandManager clientCommandManager,
            final MessageService messageService) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.clientCommandManager = clientCommandManager;
        this.messageService = messageService;

        this.reportMenu = MenuInterface.create(plugin);
        this.reportMenu.addTransformer((view, pane) -> {
            pane.setTitle("Report a player");
            pane.setContent(
                    """
                    Select a reason to report [accent]%s[].
                    [red]WARNING[]: Using this command without a valid reason will result in a warn.
                    """
                            .formatted(view.getState().get(REPORTED_PLAYER).plainName()));
            pane.addOptionRow(reportReason("Griefing"));
            pane.addOptionRow(reportReason("Cheating"));
            pane.addOptionRow(reportReason("Spamming"));
            pane.addOptionRow(reportReason("Toxicity"));
            pane.addOptionRow(reportReason("Sabotage"));
            pane.addOptionRow(MenuOption.of("[darkgray]Cancel", Action.back()));
        });

        this.playerMenu = PaginatedMenuInterface.create(plugin);
        this.playerMenu.addTransformer((view, pane) -> pane.setTitle("Select a player to report"));
        this.playerMenu.setElementProvider(() -> Groups.player);
        this.playerMenu.setElementRenderer(Player::plainName);
        this.playerMenu.setChoiceAction((view, player) ->
                this.reportMenu.open(view.getViewer(), State.create().with(REPORTED_PLAYER, player), view));
    }

    @Override
    public void onLifecycleInit() {
        // TODO Add getCommandHandler to ArcCommandManager
        Vars.netServer.clientCommands.removeCommand("vote");
        clientCommandManager.command(clientCommandManager
                .commandBuilder("votekick")
                .meta(CommandMeta.DESCRIPTION, "Votekick a player (this command is disabled).")
                .argument(PlayerArgument.of("player"))
                .handler(ctx -> this.reportMenu.open(
                        ctx.getSender().getPlayer(), State.create().with(REPORTED_PLAYER, ctx.get("player")))));

        clientCommandManager.command(clientCommandManager
                .commandBuilder("report")
                .meta(CommandMeta.DESCRIPTION, "Report a player.")
                .handler(ctx -> this.playerMenu.open(ctx.getSender().getPlayer(), State.create())));
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

        if (!this.messageService.isOperational()) {
            sender.sendMessage("[red]The report system is down, please contact an administrator.");
            return;
        }

        // One minute cooldown
        this.cooldown.set(sender, System.currentTimeMillis() + (60 * 1000L));
        this.messageService.publish(PlayerReportMessage.builder()
                .setReporterName(sender.plainName())
                .setServerIdentifier(this.configuration.getServerName())
                .setReportedPlayerName(target.plainName())
                .setReportedPlayerIp(target.ip())
                .setReportedPlayerUuid(target.uuid())
                .setReason(reason)
                .build());

        Groups.player.each(
                p -> !p.uuid().equals(sender.uuid()),
                p -> p.sendMessage("[scarlet]" + target.plainName() + " has been reported for '"
                        + reason + "' by "
                        + sender.plainName() + "."));
    }

    private MenuOption reportReason(final String reason) {
        return MenuOption.of(
                reason, view -> report(view.getViewer(), view.getState().get(REPORTED_PLAYER), reason));
    }
}
