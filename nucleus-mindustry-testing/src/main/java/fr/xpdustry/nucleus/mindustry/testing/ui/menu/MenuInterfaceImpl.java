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

import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.distributor.api.util.MUUID;
import fr.xpdustry.nucleus.mindustry.testing.ui.AbstractTransformingInterface;
import fr.xpdustry.nucleus.mindustry.testing.ui.View;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.ui.Menus;
import org.checkerframework.checker.nullness.qual.Nullable;

// TODO Transform into an abstract class to extend or simply using more complex transformers ?
final class MenuInterfaceImpl extends AbstractTransformingInterface<MenuPane> implements MenuInterface {

    private final Map<MUUID, MenuView> views = new HashMap<>();
    private final int id;
    private Action closeAction = Action.close();

    MenuInterfaceImpl(final MindustryPlugin plugin) {
        super(plugin);
        this.id = Menus.registerMenu((player, option) -> {
            final var view = this.views.get(MUUID.of(player));
            if (view == null) {
                this.getPlugin()
                        .getLogger()
                        .warn(
                                "Received menu response from player {} (uuid: {}) but no view was found",
                                player.name(),
                                player.uuid());
            } else if (option == -1) {
                this.closeAction.accept(view);
            } else {
                view.getPane().getOption(option).getAction().accept(view);
            }
        });

        DistributorProvider.get().getEventBus().subscribe(EventType.PlayerLeave.class, plugin, event -> {
            final var view = this.views.get(MUUID.of(event.player));
            if (view != null) {
                view.close();
            }
        });
    }

    @Override
    public View open(final Player viewer, final State state, final @Nullable View parent) {
        final var view = views.computeIfAbsent(MUUID.of(viewer), p -> new MenuView(viewer, parent));
        view.setState(state);
        view.update();
        return view;
    }

    @Override
    protected MenuPane createPane() {
        return new MenuPaneImpl();
    }

    @Override
    public Action getCloseAction() {
        return closeAction;
    }

    @Override
    public void setCloseAction(final Action closeAction) {
        this.closeAction = closeAction;
    }

    private final class MenuView extends AbstractView {

        private MenuView(final Player viewer, final @Nullable View parent) {
            super(viewer, parent);
        }

        @Override
        public boolean isOpen() {
            return MenuInterfaceImpl.this.views.containsKey(MUUID.of(this.getViewer()));
        }

        @Override
        public void update() {
            transform(this);
            Call.followUpMenu(
                    getViewer().con(),
                    MenuInterfaceImpl.this.id,
                    this.getPane().getTitle(),
                    this.getPane().getContent(),
                    Arrays.stream(this.getPane().getOptions())
                            .map(r ->
                                    Arrays.stream(r).map(MenuOption::getContent).toArray(String[]::new))
                            .toArray(String[][]::new));
        }

        @Override
        public void close() {
            if (isOpen()) {
                Call.hideFollowUpMenu(this.getViewer().con(), MenuInterfaceImpl.this.id);
                views.remove(MUUID.of(this.getViewer()));
            }
        }
    }
}
