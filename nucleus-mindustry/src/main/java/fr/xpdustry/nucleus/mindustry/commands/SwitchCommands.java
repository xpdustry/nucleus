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
import fr.xpdustry.nucleus.api.annotation.NucleusAutoListener;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.network.DiscoveryService;
import fr.xpdustry.nucleus.mindustry.command.CommandService;
import javax.inject.Inject;
import mindustry.gen.Call;
import org.checkerframework.checker.nullness.qual.Nullable;

@NucleusAutoListener
public final class SwitchCommands implements LifecycleListener {

    private final CommandService commandService;
    private final DiscoveryService discoveryService;

    @Inject
    public SwitchCommands(final CommandService commandService, final DiscoveryService discoveryService) {
        this.commandService = commandService;
        this.discoveryService = discoveryService;
    }

    @Override
    public void onLifecycleInit() {
        final var manager = this.commandService.getClientCommandManager();

        manager.command(manager.commandBuilder("switch")
                .meta(CommandMeta.DESCRIPTION, "Switch to another Xpdustry server.")
                .argument(StringArgument.optional("name"))
                .handler(ctx -> onSwitchCommand(ctx.getSender(), ctx.getOrDefault("name", null))));

        manager.command(manager.commandBuilder("hub")
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
