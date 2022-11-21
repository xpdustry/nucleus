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
package fr.xpdustry.nucleus.mindustry.listeners;

import arc.util.CommandHandler;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.argument.PlayerArgument;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.nucleus.common.event.ImmutablePlayerReportEvent;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
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

        manager.command(manager.commandBuilder("report")
                .meta(CommandMeta.DESCRIPTION, "Report a player.")
                .argument(PlayerArgument.of("player"))
                .argument(StringArgument.greedy("reason"))
                .handler(ctx -> {
                    final var reported = ctx.<Player>get("player");
                    if (reported.uuid().equals(ctx.getSender().getPlayer().uuid())) {
                        ctx.getSender().sendMessage("You can't report yourself >:(");
                        return;
                    }
                    JavelinPlugin.getJavelinSocket()
                            .sendEvent(ImmutablePlayerReportEvent.builder()
                                    .playerName(ctx.getSender().getPlayer().plainName())
                                    .serverName(this.nucleus.getConfiguration().getServerName())
                                    .reportedPlayerName(reported.plainName())
                                    .reportedPlayerIp(reported.ip())
                                    .reportedPlayerUuid(reported.uuid())
                                    .reason(ctx.get("reason"))
                                    .build());
                    ctx.getSender().sendMessage("Your report has been sent.");
                }));

        manager.command(manager.commandBuilder("votekick")
                .meta(CommandMeta.DESCRIPTION, "Votekick a player")
                .argument(StringArgument.greedy("player"))
                .handler(ctx -> Call.infoMessage(ctx.getSender().getPlayer().con(), VOTEKICK_DISABLED_MESSAGE)));

        manager.command(manager.commandBuilder("t")
                .meta(CommandMeta.DESCRIPTION, "Send a message to your team.")
                .argument(StringArgument.greedy("message"))
                .handler(ctx -> {
                    final var player = ctx.getSender().getPlayer();
                    final String message = ctx.get("message");
                    if (this.nucleus.getFilters().stream().allMatch(f -> f.filter(player, message))) {
                        Groups.player.each(p -> p.team() == player.team(), receiver -> {
                            var result = message;
                            for (final var processor : this.nucleus.getProcessors()) {
                                result = processor.process(player, result, receiver);
                            }
                            Call.sendMessage(
                                    receiver.con(),
                                    "[#" + player.team().color.toString() + "]<T> "
                                            + Vars.netServer.chatFormatter.format(player, result),
                                    result,
                                    player);
                        });
                    }
                }));
    }
}
