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
package fr.xpdustry.nucleus.mindustry;

import arc.util.CommandHandler;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.ArcCommandManager;
import fr.xpdustry.distributor.api.command.argument.PlayerArgument;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.event.EventBus;
import fr.xpdustry.distributor.api.event.EventBusListener;
import fr.xpdustry.distributor.api.plugin.ExtendedPlugin;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.nucleus.common.NucleusApplication;
import fr.xpdustry.nucleus.common.NucleusApplicationProvider;
import fr.xpdustry.nucleus.common.event.ImmutablePlayerReportEvent;
import fr.xpdustry.nucleus.common.util.Platform;
import fr.xpdustry.nucleus.mindustry.chat.DiscordBridge;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.net.Administration;

public final class NucleusPlugin extends ExtendedPlugin implements NucleusApplication, EventBusListener {

    private final ArcCommandManager<CommandSender> clientCommands = ArcCommandManager.standard(this);
    private final Administration.Config serverName =
            new Administration.Config("nucleus:server-name", "The internal name of this server.", "test");

    @Override
    public void onInit() {
        NucleusApplicationProvider.set(this);
        EventBus.mindustry().register(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onLoad() {
        EventBus.mindustry().register(new DiscordBridge(serverName));
    }

    @Override
    public void onClientCommandsRegistration(final CommandHandler handler) {
        this.clientCommands.initialize(handler);

        this.clientCommands.command(this.clientCommands
                .commandBuilder("discord")
                .meta(CommandMeta.DESCRIPTION, "Send you our discord invitation link.")
                .handler(ctx -> Call.openURI(ctx.getSender().getPlayer().con(), "https://discord.xpdustry.fr")));

        this.clientCommands.command(this.clientCommands
                .commandBuilder("report")
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
                                    .serverName(serverName.string())
                                    .reportedPlayerName(reported.plainName())
                                    .reportedPlayerIp(reported.ip())
                                    .reportedPlayerUuid(reported.uuid())
                                    .reason(ctx.get("reason"))
                                    .build());
                    ctx.getSender().sendMessage("Your report has been sent.");
                }));

        handler.removeCommand("votekick");
        this.clientCommands.command(
                this.clientCommands
                        .commandBuilder("votekick")
                        .meta(CommandMeta.DESCRIPTION, "Votekick a player")
                        .argument(StringArgument.greedy("player"))
                        .handler(
                                ctx -> ctx.getSender()
                                        .getPlayer()
                                        .sendMessage(
                                                """
                        [red]The votekick command is disabled in this server.[] If you want to report someone, you can :
                        - Use the [cyan]/report <player> <reason>[] command (this will call a moderator).
                        - Join our discord server ([cyan]/discord[]) and post a message with a screenshot in the [cyan]#report[] channel.
                        [gray]Thanks for your understanding.[]
                        """)));
    }

    @Override
    public Platform getPlatform() {
        return Platform.MINDUSTRY;
    }
}
