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
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.api.annotation.NucleusAutoService;
import fr.xpdustry.nucleus.api.network.DiscoveryService;
import fr.xpdustry.nucleus.mindustry.command.CommandService;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Player;

@NucleusAutoService
public final class SwitchCommands implements PluginListener {

    private final CommandService commandService;
    private final DiscoveryService discoveryService;

    @Inject
    public SwitchCommands(final CommandService commandService, final DiscoveryService discoveryService) {
        this.commandService = commandService;
        this.discoveryService = discoveryService;
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = this.commandService.getClientCommandManager();

        manager.command(manager.commandBuilder("switch")
                .meta(CommandMeta.DESCRIPTION, "Switch to another Xpdustry server.")
                .argument(StringArgument.optional("name"))
                .handler(ctx -> {
                    if (ctx.contains("name")) {
                        if (this.discoveryService.getDiscoveredServers().stream()
                                .noneMatch(server -> server.getIdentifier().equals(ctx.<String>get("name")))) {
                            ctx.getSender().sendWarning("This server is not available.");
                            return;
                        }
                        connect(ctx.getSender().getPlayer(), ctx.get("name"));
                        return;
                    }

                    final var builder = new StringBuilder();
                    builder.append("[white][cyan]-- [white]Xpdustry servers[] --[]");
                    for (final var server : this.discoveryService.getDiscoveredServers()) {
                        builder.append("\n[gray] >[] ").append(server.getIdentifier());
                    }
                    ctx.getSender().sendMessage(builder.toString());
                }));

        manager.command(manager.commandBuilder("hub")
                .meta(CommandMeta.DESCRIPTION, "Switch to the Xpdustry hub.")
                .handler(ctx -> connect(ctx.getSender().getPlayer(), "hub")));
    }

    private void connect(final Player player, final String server) {
        Vars.net.pingHost(
                server + ".md.xpdustry.fr",
                Vars.port,
                host -> {
                    Call.connect(player.con(), host.address, host.port);
                    Call.sendMessage(
                            "[accent]" + player.plainName() + "[] switched to the [cyan]" + "hub" + "[] server.");
                },
                e -> player.sendMessage("[scarlet]The server is offline or not found."));
    }
}
