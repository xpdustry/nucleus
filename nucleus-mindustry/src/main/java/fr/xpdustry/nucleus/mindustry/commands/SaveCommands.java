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
import fr.xpdustry.nucleus.testing.ui.Action;
import fr.xpdustry.nucleus.testing.ui.State;
import fr.xpdustry.nucleus.testing.ui.StateKey;
import fr.xpdustry.nucleus.testing.ui.menu.MenuInterface;
import fr.xpdustry.nucleus.testing.ui.menu.MenuOption;
import fr.xpdustry.nucleus.testing.ui.menu.MenuView;
import java.util.Arrays;
import mindustry.Vars;
import mindustry.gen.Iconc;
import mindustry.io.SaveIO;
import mindustry.net.WorldReloader;

public final class SaveCommands implements PluginListener {

    private static final StateKey<Integer> PAGE = StateKey.of("page", Integer.class);
    private static final StateKey<String> CHOICE = StateKey.of("choice", String.class);
    private static final long PAGE_SIZE = 5L;

    private final MenuInterface menu = MenuInterface.create();
    private final NucleusPlugin nucleus;

    // TODO It would be nice to create a MapManager for map handling
    public SaveCommands(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;

        this.menu.addTransformer(view -> view.getPane().setTitle("Saves"));

        this.menu.addTransformer(view -> {
            if (view.getState().contains(CHOICE)) {
                return view.getPane();
            }

            var pane = view.getPane();
            final var saves = Arrays.stream(Vars.saveDirectory.list())
                    .filter(file -> file.extension().equals(Vars.saveExtension))
                    .map(Fi::nameWithoutExtension)
                    .sorted()
                    .skip(view.getState().get(PAGE) * PAGE_SIZE)
                    .limit(PAGE_SIZE + 1) // Need to be aware if there are more elements
                    .toList();

            if (saves.isEmpty()) {
                pane = pane.addOptionRow(MenuOption.of("No saves", Action.open()));
            }
            for (int i = 0; i < PAGE_SIZE && i < saves.size(); i++) {
                final var save = saves.get(i);
                pane = pane.addOptionRow(MenuOption.of(save, Action.openWith(CHOICE, save)));
            }
            final var page = view.getState().get(PAGE);
            return pane.addOptionRow(
                    enableIf(page > 0, Iconc.left, Action.openWith(PAGE, page - 1)),
                    MenuOption.of(Iconc.cancel, Action.none()),
                    enableIf(saves.size() > PAGE_SIZE, Iconc.right, Action.openWith(PAGE, page + 1)));
        });

        this.menu.addTransformer(view -> {
            if (!view.getState().contains(CHOICE)) {
                return view.getPane();
            }
            return view.getPane()
                    .setContent(view.getState().get(CHOICE))
                    .addOptionRow(
                            MenuOption.of(
                                    "[green]" + Iconc.play,
                                    Action.command("load", view.getState().get(CHOICE))),
                            MenuOption.of("[red]" + Iconc.trash, v -> {
                                Vars.saveDirectory
                                        .child(view.getState().get(CHOICE) + "." + Vars.saveExtension)
                                        .delete();
                                Action.openWithout(CHOICE).accept(v);
                            }),
                            MenuOption.of("[gray]" + Iconc.cancel, Action.openWithout(CHOICE)));
        });
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = nucleus.getClientCommands();

        manager.command(manager.commandBuilder("saves")
                .meta(CommandMeta.DESCRIPTION, "Opens the save menu.")
                .permission("fr.xpdustry.nucleus.saves.menu")
                .handler(ctx ->
                        menu.open(ctx.getSender().getPlayer(), State.create().with(PAGE, 0))));

        manager.command(manager.commandBuilder("save")
                .permission("fr.xpdustry.nucleus.saves.save")
                .meta(CommandMeta.DESCRIPTION, "Save the current game.")
                .argument(StringArgument.of("name"))
                .handler(ctx -> {
                    final var file = Vars.saveDirectory.child(ctx.get("name") + "." + Vars.saveExtension);
                    Core.app.post(() -> {
                        SaveIO.save(file);
                        ctx.getSender().sendMessage(String.format("Saved to %s.", file));
                    });
                }));

        manager.command(manager.commandBuilder("load")
                .permission("fr.xpdustry.nucleus.saves.load")
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

    private MenuOption enableIf(final boolean active, final char icon, final Action<MenuView> action) {
        return MenuOption.of(active ? String.valueOf(icon) : "[darkgray]" + icon, active ? action : Action.open());
    }
}
