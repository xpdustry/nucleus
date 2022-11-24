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

import arc.Core;
import arc.files.Fi;
import arc.util.CommandHandler;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.internal.NucleusPluginCommandManager;
import java.util.Arrays;
import mindustry.Vars;
import mindustry.io.SaveIO;
import mindustry.net.WorldReloader;

public final class SharedCommands implements PluginListener {

    private final NucleusPlugin nucleus;

    public SharedCommands(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginServerCommandsRegistration(final CommandHandler handler) {
        this.onPluginSharedCommandsRegistration(this.nucleus.getServerCommands());
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        this.onPluginSharedCommandsRegistration(this.nucleus.getClientCommands());
    }

    private void onPluginSharedCommandsRegistration(final NucleusPluginCommandManager manager) {
        // TODO Convert into visual menu, typing the name manually is awful
        manager.command(manager.commandBuilder("saves")
                .permission("nucleus.game.saves")
                .meta(CommandMeta.DESCRIPTION, "List all the saves.")
                .handler(ctx -> {
                    ctx.getSender().sendMessage("Save files: ");
                    Arrays.stream(Vars.saveDirectory.list())
                            .filter(file -> file.extension().equals(Vars.saveExtension))
                            .map(Fi::nameWithoutExtension)
                            .sorted()
                            .forEach(name -> ctx.getSender().sendMessage("| " + name));
                }));

        manager.command(manager.commandBuilder("load")
                .permission("nucleus.game.load")
                .meta(CommandMeta.DESCRIPTION, "Load a save.")
                .argument(StringArgument.greedy("slot"))
                .handler(ctx -> {
                    final var file = Vars.saveDirectory.child(ctx.get("slot") + "." + Vars.saveExtension);

                    if (!SaveIO.isSaveValid(file)) {
                        ctx.getSender().sendWarning("No (valid) save data found for slot.");
                        return;
                    }

                    Core.app.post(() -> {
                        final var hotLoading = Vars.state.isPlaying();
                        final var reloader = new WorldReloader();

                        if (hotLoading) {
                            reloader.begin();
                        }

                        try {
                            SaveIO.load(file);
                            Vars.state.rules.sector = null;
                            ctx.getSender().sendMessage("Save loaded.");
                        } catch (final Exception exception) {
                            ctx.getSender().sendMessage("Failed to load save. Outdated or corrupt file.");
                            Vars.world.loadMap(Vars.maps.all().random());
                        } finally {
                            Vars.logic.play();
                            if (hotLoading) {
                                reloader.end();
                            } else {
                                Vars.netServer.openServer();
                            }
                        }
                    });
                }));
    }
}
