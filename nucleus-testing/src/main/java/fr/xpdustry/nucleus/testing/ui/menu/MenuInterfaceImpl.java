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
package fr.xpdustry.nucleus.testing.ui.menu;

import fr.xpdustry.nucleus.testing.ui.Action;
import fr.xpdustry.nucleus.testing.ui.State;
import fr.xpdustry.nucleus.testing.ui.Transform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.ui.Menus;

final class MenuInterfaceImpl implements MenuInterface {

    private final Map<Player, MenuViewImpl> views = new HashMap<>();
    private final List<Transform<MenuView, MenuPane>> transformers = new ArrayList<>();
    private final int id;
    private Action<MenuView> closeAction = Action.none();

    {
        this.id = Menus.registerMenu((player, option) -> {
            final var view = this.views.remove(player);
            if (view == null) {
                throw new IllegalStateException();
            } else if (option == -1) {
                this.closeAction.accept(view);
            } else {
                view.getPane().getOption(option).getAction().accept(view);
            }
        });
    }

    @Override
    public MenuView open(final Player viewer, final State state) {
        return views.computeIfAbsent(viewer, p -> {
            final var view = new MenuViewImpl(p, state.copy());
            for (final var transformer : transformers) {
                view.pane = transformer.apply(view);
            }
            final var options = view.pane instanceof MenuPaneImpl impl ? impl.options : view.pane.getOptions();
            Call.menu(
                    id,
                    view.getPane().getTitle(),
                    view.getPane().getContent(),
                    Arrays.stream(options)
                            .map(r ->
                                    Arrays.stream(r).map(MenuOption::getContent).toArray(String[]::new))
                            .toArray(String[][]::new));
            return view;
        });
    }

    @Override
    public void addTransformer(Transform<MenuView, MenuPane> transformer) {
        transformers.add(transformer);
    }

    @Override
    public List<Transform<MenuView, MenuPane>> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    @Override
    public Action<MenuView> getCloseAction() {
        return closeAction;
    }

    @Override
    public void setCloseAction(Action<MenuView> action) {
        this.closeAction = action;
    }

    private final class MenuViewImpl implements MenuView {

        private final Player viewer;
        private final State state;
        private MenuPane pane = new MenuPaneImpl();

        private MenuViewImpl(final Player viewer, final State state) {
            this.viewer = viewer;
            this.state = state;
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public MenuPane getPane() {
            return pane;
        }

        @Override
        public MenuInterface getInterface() {
            return MenuInterfaceImpl.this;
        }

        @Override
        public Player getViewer() {
            return viewer;
        }

        @Override
        public boolean isViewing() {
            return views.containsKey(viewer);
        }
    }
}
