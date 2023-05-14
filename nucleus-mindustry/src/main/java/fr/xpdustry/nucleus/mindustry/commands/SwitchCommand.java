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
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.network.DiscoveryService;
import fr.xpdustry.nucleus.common.network.MindustryServerInfo;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.ListTransformer;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuInterface;
import java.util.List;
import java.util.Map.Entry;
import javax.inject.Inject;
import mindustry.gen.Call;
import mindustry.gen.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class SwitchCommand implements NucleusListener {

    private final NucleusPluginCommandManager clientCommandManager;
    private final DiscoveryService discovery;
    private final MenuInterface list;

    @Inject
    public SwitchCommand(
            final MindustryPlugin plugin,
            final @ClientSide NucleusPluginCommandManager clientCommandManager,
            final DiscoveryService discovery) {
        this.clientCommandManager = clientCommandManager;
        this.discovery = discovery;

        this.list = MenuInterface.create(plugin)
                .addTransformer((view, pane) ->
                        pane.setTitle("[cyan]Xpdustry Servers").setContent("Select a server to switch to."))
                .addTransformer(new ListTransformer<Entry<String, MindustryServerInfo>>()
                        .setElementProvider(() ->
                                List.copyOf(discovery.getDiscoveredServers().entrySet()))
                        .setPageHeight(8)
                        .setElementRenderer(entry -> entry.getValue().getName())
                        .setChoiceAction(Action.close()
                                .<Entry<String, MindustryServerInfo>>asBiAction()
                                .then((view, entry) -> doSwitch(
                                        view.getViewer(),
                                        entry.getKey(),
                                        entry.getValue().getHost(),
                                        entry.getValue().getPort()))));
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
            list.open(sender.getPlayer());
            return;
        }

        final var servers = this.discovery.getDiscoveredServers();
        if (servers.containsKey(destination)) {
            final var server = servers.get(destination);
            doSwitch(sender.getPlayer(), destination, server.getHost(), server.getPort());
        } else {
            sender.sendWarning("The server " + destination + " is offline or not found.");
        }
    }

    private void doSwitch(final Player player, final String name, final String host, final int port) {
        Call.connect(player.con(), host, port);
        Call.sendMessage("[accent]" + player.plainName() + "[] switched to the [cyan]" + name + "[] server.");
    }
}
