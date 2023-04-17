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
import fr.xpdustry.nucleus.mindustry.testing.map.MapLoader;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuOption;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.PaginatedMenuInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.StateKey;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import mindustry.Vars;
import mindustry.gen.Iconc;
import mindustry.io.SaveIO;

public final class SaveCommand implements PluginListener {

    private static final StateKey<Fi> SAVE_FILE = StateKey.of("choice", Fi.class);

    private final PaginatedMenuInterface<Fi> menu;
    private final MenuInterface submenu;
    private final NucleusPlugin nucleus;

    // TODO It would be nice to create a MapManager for map handling
    public SaveCommand(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;

        this.submenu = MenuInterface.create(nucleus);
        this.submenu.addTransformer((view, pane) -> {
            final var save = view.getState().get(SAVE_FILE);
            pane.setContent(save.nameWithoutExtension());
            pane.addOptionRow(
                    MenuOption.of(
                            "[green]" + Iconc.play,
                            Action.closeAll().then(Action.command("load", save.nameWithoutExtension()))),
                    MenuOption.of(
                            "[red]" + Iconc.trash, Action.run(save::delete).then(Action.close())),
                    MenuOption.of("[gray]" + Iconc.cancel, Action.close()));
        });

        this.menu = PaginatedMenuInterface.create(nucleus);
        this.menu.addTransformer((view, pane) -> pane.setTitle("Saves"));
        this.menu.setChoiceAction((view, value) -> {
            this.submenu.open(view.getViewer(), State.create().with(SAVE_FILE, value), view);
        });
        this.menu.setElementRenderer(Fi::nameWithoutExtension);
        this.menu.setElementProvider(() -> Arrays.stream(Vars.saveDirectory.list())
                .filter(file -> file.extension().equals(Vars.saveExtension))
                .sorted(Comparator.comparing(Fi::nameWithoutExtension))
                .toList());
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = nucleus.getClientCommands();

        // TODO Add intermediary permission for save deletion
        manager.command(manager.commandBuilder("saves")
                .meta(CommandMeta.DESCRIPTION, "Opens the save menu.")
                .permission("nucleus.saves")
                .handler(ctx -> menu.open(ctx.getSender().getPlayer(), State.create())));

        manager.command(manager.commandBuilder("save")
                .permission("nucleus.saves.save")
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
                .permission("nucleus.saves.load")
                .meta(CommandMeta.DESCRIPTION, "Load a save.")
                .argument(StringArgument.greedy("slot"))
                .handler(ctx -> {
                    final var slot = ctx.<String>get("slot");
                    final var fi = Vars.saveDirectory.child(slot + "." + Vars.saveExtension);
                    if (!SaveIO.isSaveValid(fi)) {
                        ctx.getSender().sendWarning("No (valid) save data found for slot.");
                        return;
                    }
                    Core.app.post(() -> {
                        try (final var loader = MapLoader.create()) {
                            loader.load(fi.file());
                            this.nucleus.getLogger().info("Save {} loaded.", slot);
                        } catch (final IOException exception) {
                            this.nucleus
                                    .getLogger()
                                    .error("Failed to load save {} (Outdated or corrupt file).", slot, exception);
                        }
                    });
                }));
    }
}
