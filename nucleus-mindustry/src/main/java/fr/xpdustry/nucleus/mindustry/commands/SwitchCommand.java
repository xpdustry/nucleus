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
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import mindustry.Vars;
import mindustry.gen.Call;

public final class SwitchCommand implements PluginListener {

    private final NucleusPlugin nucleus;

    public SwitchCommand(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = this.nucleus.getClientCommands();

        manager.command(manager.commandBuilder("switch")
                .meta(CommandMeta.DESCRIPTION, "Switch to another Xpdustry server.")
                .argument(StringArgument.optional("name"))
                .handler(ctx -> {
                    if (ctx.contains("name")) {
                        if (!this.nucleus
                                .getServerListProvider()
                                .getAvailableServers()
                                .contains(ctx.<String>get("name"))) {
                            ctx.getSender().sendWarning("This server is not available.");
                            return;
                        }
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

                    final var builder = new StringBuilder();
                    builder.append("[white][cyan]-- [white]Xpdustry servers[] --[]");
                    for (final var server : this.nucleus.getServerListProvider().getAvailableServers()) {
                        builder.append("\n[gray] >[] ").append(server);
                    }
                    ctx.getSender().sendMessage(builder.toString());
                }));
    }
}
