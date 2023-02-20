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
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.core.event.ImmutablePlayerReportEvent;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.util.PlayerMap;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public final class PlayerCommands implements PluginListener {

    private static final String VOTEKICK_DISABLED_MESSAGE =
            """
                    [red]The votekick command is disabled in this server.[]
                    If you want to report someone, you can either :
                    - Use the [cyan]/report <player> <reason>[] command (such as [cyan]/report badguy griefing[]).
                    - Join our discord server ([cyan]/discord[]) and post a message with a screenshot in the [cyan]#report[] channel.
                    [gray]Thanks for your understanding.[]
                    """;

    private final NucleusPlugin nucleus;

    public PlayerCommands(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
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

        // TODO Maybe use TimeKeeper ?
        final var cooldown = PlayerMap.<Long>create();
        manager.command(manager.commandBuilder("report")
                .meta(CommandMeta.DESCRIPTION, "Report a player.")
                .argument(PlayerArgument.of("player"))
                .argument(StringArgument.greedy("reason"))
                .handler(ctx -> {
                    if (cooldown.get(ctx.getSender().getPlayer(), 0L) - System.currentTimeMillis() > 0) {
                        ctx.getSender()
                                .sendWarning("Ayo, chill the fuck up, wait a minute before reporting someone again.");
                        return;
                    }
                    final var reported = ctx.<Player>get("player");
                    if (reported.uuid().equals(ctx.getSender().getPlayer().uuid())) {
                        ctx.getSender().sendWarning("You can't report yourself >:(");
                        return;
                    }
                    if (!this.nucleus.getMessenger().isOpen()) {
                        ctx.getSender().sendWarning("The report system is down, please contact an administrator.");
                        return;
                    }

                    // One minute cooldown
                    cooldown.set(ctx.getSender().getPlayer(), System.currentTimeMillis() + (60 * 1000L));
                    this.nucleus
                            .getMessenger()
                            .send(ImmutablePlayerReportEvent.builder()
                                    .playerName(ctx.getSender().getPlayer().plainName())
                                    .serverName(this.nucleus.getConfiguration().getServerName())
                                    .reportedPlayerName(reported.plainName())
                                    .reportedPlayerIp(reported.ip())
                                    .reportedPlayerUuid(reported.uuid())
                                    .reason(ctx.get("reason"))
                                    .build());
                    Groups.player.each(
                            p -> !p.uuid().equals(reported.uuid()),
                            p -> p.sendMessage("[scarlet]" + reported.plainName() + " has been reported for '"
                                    + ctx.get("reason") + "' by "
                                    + ctx.getSender().getPlayer().plainName() + "."));
                }));

        manager.command(manager.commandBuilder("votekick")
                .meta(CommandMeta.DESCRIPTION, "Votekick a player (this command is disabled).")
                .argument(StringArgument.greedy("player"))
                .handler(ctx -> Call.infoMessage(ctx.getSender().getPlayer().con(), VOTEKICK_DISABLED_MESSAGE)));

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
                .argument(StringArgument.greedy("message"))
                .handler(ctx -> this.nucleus
                        .getChatManager()
                        .sendMessage(
                                ctx.getSender().getPlayer(), ctx.get("message"), p -> true, r -> r + " ¯\\_(ツ)_/¯")));
    }
}
