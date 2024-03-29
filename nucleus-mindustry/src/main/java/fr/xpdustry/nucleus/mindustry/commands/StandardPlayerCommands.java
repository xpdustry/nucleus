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

import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.argument.PlayerArgument;
import fr.xpdustry.distributor.api.command.argument.TeamArgument;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.chat.ChatManager;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import javax.inject.Inject;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;

public final class StandardPlayerCommands implements NucleusListener {

    private static final String SHRUG = "¯\\_(ツ)_/¯";

    private final ChatManager chatManager;
    private final NucleusPluginCommandManager clientCommandManager;

    @Inject
    public StandardPlayerCommands(
            final ChatManager chatManager, final @ClientSide NucleusPluginCommandManager clientCommandManager) {
        this.chatManager = chatManager;
        this.clientCommandManager = clientCommandManager;
    }

    @Override
    public void onNucleusInit() {
        clientCommandManager.command(clientCommandManager
                .commandBuilder("discord")
                .meta(CommandMeta.DESCRIPTION, "Send you our discord invitation link.")
                .handler(ctx -> Call.openURI(ctx.getSender().getPlayer().con(), "https://discord.xpdustry.fr")));

        clientCommandManager.command(clientCommandManager
                .commandBuilder("website")
                .meta(CommandMeta.DESCRIPTION, "Send you our website link.")
                .handler(ctx -> Call.openURI(ctx.getSender().getPlayer().con(), "https://www.xpdustry.fr")));

        clientCommandManager.command(clientCommandManager
                .commandBuilder("t")
                .meta(CommandMeta.DESCRIPTION, "Send a message to your team.")
                .argument(StringArgument.greedy("message"))
                .handler(ctx -> {
                    final var player = ctx.getSender().getPlayer();
                    this.chatManager.sendMessage(
                            ctx.getSender().getPlayer(),
                            ctx.get("message"),
                            p -> p.team().equals(player.team()),
                            r -> "[#" + player.team().color.toString() + "]<T>[] " + r);
                }));

        clientCommandManager.command(clientCommandManager
                .commandBuilder("w")
                .meta(CommandMeta.DESCRIPTION, "Send a private message to a player.")
                .argument(PlayerArgument.of("player"))
                .argument(StringArgument.greedy("message"))
                .handler(ctx -> {
                    final var player = ctx.getSender().getPlayer();
                    final var target = ctx.<Player>get("player");
                    this.chatManager.sendMessage(
                            player, ctx.get("message"), p -> p.equals(target), r -> "[gray]<W>[] " + r);
                }));

        clientCommandManager.command(clientCommandManager
                .commandBuilder("shrug")
                .meta(CommandMeta.DESCRIPTION, "Send a shrug.")
                .argument(StringArgument.<CommandSender>builder("message")
                        .greedy()
                        .asOptional())
                .handler(ctx -> this.chatManager.sendMessage(
                        ctx.getSender().getPlayer(),
                        ctx.getOrDefault("message", ""),
                        p -> true,
                        r -> r.isBlank() ? SHRUG : r + SHRUG)));

        clientCommandManager.command(clientCommandManager
                .commandBuilder("team")
                .permission("nucleus.team")
                .meta(CommandMeta.DESCRIPTION, "Change your team.")
                .argument(TeamArgument.all("team"))
                .handler(ctx -> {
                    final var player = ctx.getSender().getPlayer();
                    final var team = ctx.<Team>get("team");
                    player.team(team);
                    ctx.getSender().sendMessage("You are now in the team " + team.name);
                }));
    }
}
