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

import arc.util.CommandHandler;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.argument.PlayerArgument;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.util.PlayerLookup;
import fr.xpdustry.nucleus.core.event.ImmutablePlayerReportEvent;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.testing.ui.State;
import fr.xpdustry.nucleus.mindustry.testing.ui.StateKey;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuOption;
import fr.xpdustry.nucleus.mindustry.util.PlayerMap;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public final class PlayerCommands implements PluginListener {

    private static final String SHRUG = "¯\\_(ツ)_/¯";
    private static final String VOTEKICK_DISABLED_MESSAGE =
            """
                    [red]The votekick command is disabled in this server.[]
                    If you want to report someone, you can either :
                    - Use the [cyan]/report <player> <reason>[] command (such as [cyan]/report badguy griefing[]).
                    - Join our discord server ([cyan]/discord[]) and post a message with a screenshot in the [cyan]#report[] channel.
                    [gray]Thanks for your understanding.[]
                    """;

    private final StateKey<Player> REPORTED_PLAYER = StateKey.of("reported_player", Player.class);
    private final NucleusPlugin nucleus;
    private final MenuInterface votekickMenu = MenuInterface.create();

    public PlayerCommands(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginInit() {
        // TODO This is a mess, move the votekick command to a dedicated class.
        this.votekickMenu.addTransformer(view -> view.getPane()
                .setContent("[red]You want to kick "
                        + view.getState().get(REPORTED_PLAYER).plainName() + " ? Well, what's the reason ?.[]")
                .addOptionRow(votekickReason("Griefing"))
                .addOptionRow(votekickReason("Cheating"))
                .addOptionRow(votekickReason("Spamming"))
                .addOptionRow(votekickReason("Toxicity"))
                .addOptionRow(votekickReason("Other")));
    }

    private MenuOption votekickReason(final String reason) {
        return MenuOption.of(
                reason, view -> report(view.getViewer(), view.getState().get(REPORTED_PLAYER), reason));
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = this.nucleus.getClientCommands();

        manager.command(manager.commandBuilder("discord")
                .meta(CommandMeta.DESCRIPTION, "Send you our discord invitation link.")
                .handler(ctx -> Call.openURI(ctx.getSender().getPlayer().con(), "https://discord.xpdustry.fr")));

        manager.command(manager.commandBuilder("website")
                .meta(CommandMeta.DESCRIPTION, "Send you our website link.")
                .handler(ctx -> Call.openURI(ctx.getSender().getPlayer().con(), "https://www.xpdustry.fr")));

        manager.command(manager.commandBuilder("report")
                .meta(CommandMeta.DESCRIPTION, "Report a player.")
                .argument(PlayerArgument.of("player"))
                .argument(StringArgument.greedy("reason"))
                .handler(ctx -> this.report(ctx.getSender().getPlayer(), ctx.get("player"), ctx.get("reason"))));

        manager.command(manager.commandBuilder("votekick")
                .meta(CommandMeta.DESCRIPTION, "Votekick a player (this command is disabled).")
                .argument(StringArgument.greedy("player"))
                .handler(ctx -> {
                    final var result = PlayerLookup.findPlayers(ctx.get("player"), true);
                    if (result.isEmpty()) {
                        ctx.getSender().sendWarning("No player found.");
                    } else if (result.size() > 1) {
                        ctx.getSender().sendWarning("Too many players found.");
                    } else {
                        this.votekickMenu.open(
                                ctx.getSender().getPlayer(), State.create().with(REPORTED_PLAYER, result.get(0)));
                    }
                }));

        manager.command(manager.commandBuilder("vote")
                .meta(CommandMeta.DESCRIPTION, "Vote to kick the current player (this command is disabled).")
                .argument(StringArgument.of("y/n"))
                .handler(ctx -> Call.infoMessage(ctx.getSender().getPlayer().con(), VOTEKICK_DISABLED_MESSAGE)));

        manager.command(manager.commandBuilder("t")
                .meta(CommandMeta.DESCRIPTION, "Send a message to your team.")
                .argument(StringArgument.greedy("message"))
                .handler(ctx -> {
                    final var player = ctx.getSender().getPlayer();
                    this.nucleus
                            .getChatManager()
                            .sendMessage(
                                    ctx.getSender().getPlayer(),
                                    ctx.get("message"),
                                    p -> p.team().equals(player.team()),
                                    r -> "[#" + player.team().color.toString() + "]<T>[] " + r);
                }));

        manager.command(manager.commandBuilder("w")
                .meta(CommandMeta.DESCRIPTION, "Send a private message to a player.")
                .argument(PlayerArgument.of("player"))
                .argument(StringArgument.greedy("message"))
                .handler(ctx -> {
                    final var player = ctx.getSender().getPlayer();
                    final var target = ctx.<Player>get("player");
                    this.nucleus
                            .getChatManager()
                            .sendMessage(player, ctx.get("message"), p -> p.equals(target), r -> "[gray]<W>[] " + r);
                }));

        manager.command(manager.commandBuilder("switch")
                .meta(CommandMeta.DESCRIPTION, "Switch to another Xpdustry server.")
                .argument(StringArgument.optional("name"))
                .handler(ctx -> {
                    if (ctx.contains("name")) {
                        Vars.net.pingHost(
                                ctx.get("name") + ".md.xpdustry.fr",
                                Vars.port,
                                host -> {
                                    Call.connect(ctx.getSender().getPlayer().con(), host.address, host.port);
                                    Call.sendMessage("[accent]"
                                            + ctx.getSender().getPlayer().plainName() + "[] switched to the [cyan]"
                                            + ctx.get("name") + "[] server.");
                                },
                                e -> ctx.getSender().sendWarning("Server offline or not found."));
                        return;
                    }
                    // TODO Use messenger to collect online servers instead of hardcoding them
                    ctx.getSender()
                            .sendMessage(
                                    """
                                    [white][cyan]-- [white]Xpdustry servers[] --[]
                                    [gray] >[] lobby
                                    [gray] >[] survival
                                    [gray] >[] router
                                    [gray] >[] attack
                                    [gray] >[] sandbox
                                    [gray] >[] pvp
                                    [gray] >[] event
                                    """);
                }));

        manager.command(manager.commandBuilder("shrug")
                .meta(CommandMeta.DESCRIPTION, "Send a shrug.")
                .argument(StringArgument.<CommandSender>builder("message")
                        .greedy()
                        .asOptional())
                .handler(ctx -> this.nucleus
                        .getChatManager()
                        .sendMessage(
                                ctx.getSender().getPlayer(),
                                ctx.getOrDefault("message", ""),
                                p -> true,
                                r -> r.isBlank() ? SHRUG : r + SHRUG)));
    }

    // TODO Fix this goofy ass cooldown system
    private final PlayerMap<Long> cooldown = PlayerMap.create();

    private void report(final Player sender, final Player target, final String reason) {
        if (this.cooldown.get(sender, 0L) - System.currentTimeMillis() > 0) {
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
        Groups.player.each(
                p -> !p.uuid().equals(sender.uuid()),
                p -> p.sendMessage("[scarlet]" + target.plainName() + " has been reported for '"
                        + reason + "' by "
                        + sender.plainName() + "."));
    }
}
