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
import arc.util.Strings;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.mindustry.testing.map.MapLoader;
import fr.xpdustry.nucleus.mindustry.testing.ui.Interface;
import fr.xpdustry.nucleus.mindustry.testing.ui.View;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.input.TextInputInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.ListTransformer;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.menu.MenuOption;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.StateKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import mindustry.Vars;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import mindustry.io.SaveIO;
import panda.std.Result;

final class SaveInterface implements Interface {

    private static final StateKey<Path> SAVE_FILE = StateKey.of("choice", Path.class);
    private static final Pattern SAVE_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]+$");
    private static final String SAVE_EXTENSION = "msav";
    private static final String SAVE_BACKUP_EXTENSION = "msav-backup.msav";

    private final TextInputInterface renameSave;
    private final TextInputInterface createSave;
    private final MenuInterface saveMenu;
    private final MenuInterface saveList;
    private final MindustryPlugin plugin;

    SaveInterface(final MindustryPlugin plugin) {
        this.plugin = plugin;

        this.renameSave = TextInputInterface.create(plugin)
                .setMaxInputLength(32)
                .addTransformer((view, pane) ->
                        pane.setTitle("Rename the save file").setContent("Only alphanumeric characters are allowed."))
                .setInputAction(this::renameSave);

        this.createSave = TextInputInterface.create(plugin)
                .setMaxInputLength(32)
                .addTransformer((view, pane) ->
                        pane.setTitle("Create a new save file").setContent("Only alphanumeric characters are allowed."))
                .setInputAction(this::createSave);

        this.saveMenu = MenuInterface.create(plugin).addTransformer((view, pane) -> {
            final var save = view.getState().get(SAVE_FILE).orElseThrow();
            pane.setContent(this.getPathNameWithoutExtension(save))
                    .addOptionRow(
                            MenuOption.of("[red]" + Iconc.trash, this::deleteSave),
                            MenuOption.of(
                                    "[orange]" + Iconc.pencil,
                                    v -> this.renameSave.open(view, state -> state.set(SAVE_FILE, save))),
                            MenuOption.of("[green]" + Iconc.play, this::loadSave),
                            MenuOption.of(Iconc.cancel, Action.back()));
        });

        this.saveList = MenuInterface.create(plugin)
                .addTransformer((view, pane) -> pane.setTitle("Saves"))
                .addTransformer(new ListTransformer<Path>()
                        .setElementRenderer(this::getPathNameWithoutExtension)
                        .setElementProvider(this::getSaveFiles)
                        .setFillEmpty(true)
                        .setChoiceAction(
                                (parent, path) -> this.saveMenu.open(parent, state -> state.set(SAVE_FILE, path))))
                // Inserts a small add button next to the exit button of the list transformer
                .addTransformer((view, pane) -> pane.addOption(
                        2, pane.getOptions().size() - 1, MenuOption.of(Iconc.add, this.createSave::open)));
    }

    @Override
    public View create(final Player viewer) {
        return saveList.create(viewer);
    }

    @Override
    public View create(final View parent) {
        return saveList.create(parent);
    }

    private Result<Path, String> validateSave(final String name) {
        if (!SAVE_PATTERN.matcher(name).matches()) {
            return Result.error("[scarlet]Invalid name.");
        }
        final var path = Vars.saveDirectory
                .child(name.replaceAll("\\s", "_") + "." + SAVE_EXTENSION)
                .file()
                .toPath();
        if (Files.exists(path)) {
            return Result.error("[scarlet]A save file with this name already exists.");
        }
        return Result.ok(path);
    }

    private void renameSave(final View view, final String name) {
        final var result = validateSave(name);
        if (result.isErr()) {
            Action.open().then(Action.info(result.getError())).accept(view);
            return;
        }
        try {
            Files.move(view.getState().get(SAVE_FILE).orElseThrow(), result.get());
            Action.back(2).accept(view);
        } catch (final IOException exception) {
            Action.back()
                    .then(Action.info(
                            "[scarlet]An error occurred while moving the save: " + Strings.neatError(exception)))
                    .accept(view);
        }
    }

    private void createSave(final View view, final String name) {
        final var result = validateSave(name);
        if (result.isErr()) {
            Action.open().then(Action.info(result.getError())).accept(view);
            return;
        }
        view.close(); // The default behavior of textInput when no action is taken is reopening, prevent that
        Core.app.post(() -> {
            try {
                SaveIO.save(new Fi(result.get().toFile()));
                Action.back()
                        .then(Action.info(
                                "[accent]Current game saved to " + result.get().getFileName() + "."))
                        .accept(view);
            } catch (final Exception exception) {
                Action.back()
                        .then(Action.info(
                                "[scarlet]An error occurred while creating the save: " + Strings.neatError(exception)))
                        .accept(view);
            }
        });
    }

    private void deleteSave(final View view) {
        final var save = view.getState().get(SAVE_FILE).orElseThrow();
        try {
            Files.delete(save);
            Action.back().accept(view);
        } catch (final IOException exception) {
            Action.back()
                    .then(Action.info(
                            "[scarlet]An error occurred while deleting the save: " + Strings.neatError(exception)))
                    .accept(view);
        }
    }

    private void loadSave(final View view) {
        final var save = view.getState().get(SAVE_FILE).orElseThrow();
        final var fi = new Fi(save.toFile());
        if (!SaveIO.isSaveValid(fi)) {
            Action.back()
                    .then(Action.info("[scarlet]No (valid) save data found for slot."))
                    .accept(view);
            return;
        }

        Action.closeAll().accept(view);
        Core.app.post(() -> {
            try (final var loader = MapLoader.create()) {
                loader.load(save.toFile());
                this.plugin.getLogger().info("Save {} loaded.", save.getFileName());
            } catch (final IOException exception) {
                this.plugin
                        .getLogger()
                        .error("Failed to load save {} (Outdated or corrupt file).", save.getFileName(), exception);
            }
        });
    }

    private String getPathNameWithoutExtension(final Path path) {
        final var name = path.getFileName().toString();
        final var index = name.lastIndexOf('.');
        return name.substring(0, index != -1 ? index : name.length());
    }

    private List<Path> getSaveFiles() {
        return Arrays.stream(Vars.saveDirectory.list())
                .filter(file ->
                        file.name().endsWith(SAVE_EXTENSION) && !file.name().endsWith(SAVE_BACKUP_EXTENSION))
                .sorted(Comparator.comparing(Fi::nameWithoutExtension))
                .map(file -> file.file().toPath())
                .toList();
    }
}
