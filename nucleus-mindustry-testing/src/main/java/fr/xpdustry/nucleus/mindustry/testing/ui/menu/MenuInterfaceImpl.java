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
package fr.xpdustry.nucleus.mindustry.testing.ui.menu;

import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.mindustry.testing.ui.AbstractTransformerInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import mindustry.gen.Call;
import mindustry.ui.Menus;

final class MenuInterfaceImpl extends AbstractTransformerInterface<MenuInterface, MenuPane> implements MenuInterface {

    private final int id;
    private Action exitAction = Action.close();

    MenuInterfaceImpl(final MindustryPlugin plugin) {
        super(plugin);

        this.id = Menus.registerMenu((player, option) -> {
            final var view = this.getView(player);
            if (view == null) {
                this.getPlugin()
                        .getLogger()
                        .warn(
                                "Received menu response from player {} (uuid: {}) but no view was found",
                                player.plainName(),
                                player.uuid());
            } else if (option == -1) {
                this.exitAction.accept(view);
            } else {
                view.getPane()
                        .getOption(option)
                        .ifPresentOrElse(o -> o.getAction().accept(view), () -> this.getPlugin()
                                .getLogger()
                                .warn(
                                        "Received invalid menu option {} from player {} (uuid: {})",
                                        option,
                                        player.plainName(),
                                        player.uuid()));
            }
        });
    }

    @Override
    protected void onViewOpen(final SimpleView view) {
        Call.followUpMenu(
                view.getViewer().con(),
                MenuInterfaceImpl.this.id,
                view.getPane().getTitle(),
                view.getPane().getContent(),
                view.getPane().getOptions().stream()
                        .map(row -> row.stream().map(MenuOption::getContent).toArray(String[]::new))
                        .toArray(String[][]::new));
    }

    @Override
    protected void onViewClose(final SimpleView view) {
        Call.hideFollowUpMenu(view.getViewer().con(), MenuInterfaceImpl.this.id);
    }

    @Override
    protected MenuPane createPane() {
        return new MenuPaneImpl();
    }

    @Override
    public Action getExitAction() {
        return exitAction;
    }

    @Override
    public MenuInterface setExitAction(final Action exitAction) {
        this.exitAction = exitAction;
        return this;
    }
}
