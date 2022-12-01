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
package fr.xpdustry.nucleus.mindustry.ui.menu;

import fr.xpdustry.nucleus.mindustry.ui.Action;
import fr.xpdustry.nucleus.mindustry.ui.State;
import fr.xpdustry.nucleus.mindustry.ui.Transform;
import fr.xpdustry.nucleus.mindustry.ui.TransformContext;
import java.util.*;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.ui.Menus;

final class MenuInterfaceImpl implements MenuInterface {

    private final Map<Player, MenuViewImpl> views = new HashMap<>();
    private final List<Transform<MenuPane.Mutable>> transforms = new ArrayList<>();
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
    public MenuView open(Player viewer, State state) {
        return views.computeIfAbsent(viewer, p -> {
            final var view = new MenuViewImpl(p, state);
            final var context =
                    TransformContext.<MenuPane.Mutable>of(view.getState(), view.getPane(), view.getViewer());
            for (final var transform : transforms) {
                transform.accept(context);
            }
            Call.menu(
                    id,
                    view.getPane().getTitle(),
                    view.getPane().getContent(),
                    Arrays.stream(view.getPane().options)
                            .map(r ->
                                    Arrays.stream(r).map(MenuOption::getContent).toArray(String[]::new))
                            .toArray(String[][]::new));
            return view;
        });
    }

    @Override
    public void addTransformer(Transform<MenuPane.Mutable> transform) {
        transforms.add(transform);
    }

    @Override
    public List<Transform<MenuPane.Mutable>> getTransformers() {
        return Collections.unmodifiableList(transforms);
    }

    @Override
    public void setCloseAction(Action<MenuView> action) {
        this.closeAction = action;
    }

    private final class MenuViewImpl implements MenuView {

        private final MenuPaneImpl pane = new MenuPaneImpl();
        private final Player viewer;
        private final State state;

        private MenuViewImpl(final Player viewer, final State state) {
            this.viewer = viewer;
            this.state = state;
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public MenuPaneImpl getPane() {
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
