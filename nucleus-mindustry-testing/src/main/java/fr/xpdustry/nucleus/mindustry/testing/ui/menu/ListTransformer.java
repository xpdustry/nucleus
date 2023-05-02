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

import fr.xpdustry.nucleus.mindustry.testing.ui.View;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.Action;
import fr.xpdustry.nucleus.mindustry.testing.ui.action.BiAction;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.StateKey;
import fr.xpdustry.nucleus.mindustry.testing.ui.transform.Transformer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import mindustry.gen.Iconc;

public final class ListTransformer<E> implements Transformer<MenuPane> {

    public static StateKey<Integer> PAGE = StateKey.of("nucleus:pagination-transformer-page", Integer.class);

    private Supplier<List<E>> elementProvider = Collections::emptyList;
    private Function<E, String> renderer = Objects::toString;
    private BiAction<E> choice = Action.none().asBiAction();
    private int pageHeight = 5;
    private int pageWidth = 1;
    private boolean fillEmpty = false;

    @Override
    public void transform(final View view, final MenuPane pane) {
        final var elements = elementProvider.get();
        if (elements.isEmpty()) {
            pane.addOptionRow(MenuOption.of("Nothing", Action.none()));
            renderNavigation(pane, 0, false);
            return;
        }

        var page = view.getState().get(PAGE, 0);
        while (page > 0 && page * getPageSize() >= elements.size()) {
            page--;
        }
        int cursor = 0;

        for (int i = 0; i < pageHeight; i++) {
            final List<MenuOption> options = new ArrayList<>();

            for (int j = 0; j < pageWidth; j++) {
                cursor = (page * getPageSize()) + (i * pageWidth) + j;

                if (cursor < elements.size()) {
                    final var element = elements.get(cursor);
                    options.add(MenuOption.of(renderer.apply(element), v -> choice.accept(v, element)));
                } else if (fillEmpty) {
                    options.add(MenuOption.empty());
                } else {
                    break;
                }
            }

            if (!options.isEmpty()) {
                pane.addOptionRow(options);
            }

            if (cursor >= elements.size() && !fillEmpty) {
                break;
            }
        }

        renderNavigation(pane, page, cursor + 1 < elements.size());
    }

    private void renderNavigation(final MenuPane pane, final int page, final boolean hasNext) {
        pane.addOptionRow(
                enableIf(page > 0, Iconc.left, Action.open(state -> state.set(PAGE, page - 1))),
                MenuOption.of(Iconc.cancel, Action.back()),
                enableIf(hasNext, Iconc.right, Action.open(state -> state.set(PAGE, page + 1))));
    }

    public Supplier<List<E>> getElementProvider() {
        return elementProvider;
    }

    public ListTransformer<E> setElementProvider(final Supplier<List<E>> provider) {
        this.elementProvider = provider;
        return this;
    }

    public Function<E, String> getElementRenderer() {
        return renderer;
    }

    public ListTransformer<E> setElementRenderer(final Function<E, String> renderer) {
        this.renderer = renderer;
        return this;
    }

    public BiAction<E> getChoiceAction() {
        return choice;
    }

    public ListTransformer<E> setChoiceAction(final BiAction<E> action) {
        this.choice = action;
        return this;
    }

    public int getPageHeight() {
        return this.pageHeight;
    }

    public ListTransformer<E> setPageHeight(final int pageHeight) {
        this.pageHeight = pageHeight;
        return this;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public ListTransformer<E> setPageWidth(final int pageWidth) {
        this.pageWidth = pageWidth;
        return this;
    }

    public int getPageSize() {
        return this.pageHeight * this.pageWidth;
    }

    public boolean getFillEmpty() {
        return fillEmpty;
    }

    public ListTransformer<E> setFillEmpty(final boolean fillEmpty) {
        this.fillEmpty = fillEmpty;
        return this;
    }

    private MenuOption enableIf(final boolean active, final char icon, final Action action) {
        return MenuOption.of(active ? String.valueOf(icon) : "[darkgray]" + icon, active ? action : Action.open());
    }
}
