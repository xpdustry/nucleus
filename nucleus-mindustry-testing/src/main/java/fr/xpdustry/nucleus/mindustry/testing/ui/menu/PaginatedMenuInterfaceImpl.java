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
import fr.xpdustry.distributor.api.util.Priority;
import fr.xpdustry.nucleus.mindustry.testing.ui.View;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.BiAction;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import fr.xpdustry.nucleus.mindustry.testing.ui.transform.Transform;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

final class PaginatedMenuInterfaceImpl<E> implements PaginatedMenuInterface<E> {

    private final MenuInterface menu;
    private Supplier<Iterable<E>> elements = Collections::emptyList;
    private Function<E, String> renderer = Objects::toString;
    private BiAction<E> choice = BiAction.none();
    private int pageSize = 5;

    PaginatedMenuInterfaceImpl(final MindustryPlugin plugin) {
        this.menu = MenuInterface.create(plugin);

        this.menu.addTransformer((view, pane) -> {
            final var iterator = elements.get().iterator();
            if (!iterator.hasNext()) {
                pane.addOptionRow(MenuOption.of("Nothing", Action.open()));
            }
            final var page = view.getState().get(PAGE, 0);
            for (int i = 0; i < page * pageSize; i++) {
                iterator.next();
            }
            for (int i = 0; i < pageSize && iterator.hasNext(); i++) {
                final var element = iterator.next();
                pane.addOptionRow(MenuOption.of(renderer.apply(element), v -> choice.accept(v, element)));
            }
            pane.addOptionRow(
                    enableIf(
                            page > 0,
                            Iconc.left,
                            Action.openWithState(State.create().with(PAGE, page - 1))),
                    MenuOption.of(Iconc.cancel, Action.none()),
                    enableIf(
                            iterator.hasNext(),
                            Iconc.right,
                            Action.openWithState(State.create().with(PAGE, page + 1))));
        });
    }

    @Override
    public View open(final Player viewer, final State state, final @Nullable View parent) {
        return this.menu.open(viewer, state, parent);
    }

    @Override
    public List<Transform<MenuPane>> getTransformers() {
        return this.menu.getTransformers();
    }

    @Override
    public void addTransformer(final Priority priority, final Transform<MenuPane> transform) {
        this.menu.addTransformer(priority, transform);
    }

    @Override
    public Action getCloseAction() {
        return this.menu.getCloseAction();
    }

    @Override
    public void setCloseAction(final Action action) {
        this.menu.setCloseAction(action);
    }

    @Override
    public Supplier<Iterable<E>> getElementProvider() {
        return elements;
    }

    @Override
    public void setElementProvider(final Supplier<Iterable<E>> provider) {
        this.elements = provider;
    }

    @Override
    public Function<E, String> getElementRenderer() {
        return renderer;
    }

    @Override
    public void setElementRenderer(final Function<E, String> renderer) {
        this.renderer = renderer;
    }

    @Override
    public BiAction<E> getChoiceAction() {
        return choice;
    }

    @Override
    public void setChoiceAction(final BiAction<E> action) {
        this.choice = action;
    }

    @Override
    public MindustryPlugin getPlugin() {
        return this.menu.getPlugin();
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public void setPageSize(int size) {
        this.pageSize = size;
    }

    private MenuOption enableIf(final boolean active, final char icon, final Action action) {
        return MenuOption.of(active ? String.valueOf(icon) : "[darkgray]" + icon, active ? action : Action.open());
    }
}
