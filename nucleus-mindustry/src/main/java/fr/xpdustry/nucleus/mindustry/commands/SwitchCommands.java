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
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.inject.EnableScanning;
import fr.xpdustry.nucleus.common.network.DiscoveryService;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import javax.inject.Inject;
import mindustry.gen.Call;
import org.checkerframework.checker.nullness.qual.Nullable;

@EnableScanning
public final class SwitchCommands implements NucleusListener {

    private final NucleusPluginCommandManager clientCommandManager;
    private final DiscoveryService discoveryService;

    @Inject
    public SwitchCommands(
            final @ClientSide NucleusPluginCommandManager clientCommandManager,
            final DiscoveryService discoveryService) {
        this.clientCommandManager = clientCommandManager;
        this.discoveryService = discoveryService;
    }

    @Override
    public void onNucleusInit() {
        clientCommandManager.command(clientCommandManager
                .commandBuilder("switch")
                .meta(CommandMeta.DESCRIPTION, "Switch to another Xpdustry server.")
                .argument(StringArgument.optional("name"))
                .handler(ctx -> onSwitchCommand(ctx.getSender(), ctx.getOrDefault("name", null))));

        clientCommandManager.command(clientCommandManager
                .commandBuilder("hub")
                .meta(CommandMeta.DESCRIPTION, "Switch to the Xpdustry hub.")
                .handler(ctx -> onSwitchCommand(ctx.getSender(), "hub")));
    }

    private void onSwitchCommand(final CommandSender sender, final @Nullable String destination) {
        if (destination == null) {
            final var builder = new StringBuilder();
            builder.append("[white][cyan]-- [white]Xpdustry servers[] --[]");
            this.discoveryService.getDiscoveredServers().keySet().stream()
                    .sorted()
                    .forEach(server -> builder.append("\n[gray] >[] ").append(server));
            sender.sendMessage(builder.toString());
            return;
        }
        final var servers = this.discoveryService.getDiscoveredServers();
        if (servers.containsKey(destination)) {
            final var server = servers.get(destination);
            Call.connect(sender.getPlayer().con(), server.getHost(), server.getPort());
            Call.sendMessage("[accent]" + sender.getPlayer().plainName() + "[] switched to the [cyan]" + destination
                    + "[] server.");
        } else {
            sender.sendMessage("[scarlet]The server " + destination + " is offline or not found.");
        }
    }
}
