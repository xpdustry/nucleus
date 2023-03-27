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
import fr.xpdustry.nucleus.mindustry.testing.ui.action.BiAction;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.StateKey;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PaginatedMenuInterface<E> extends MenuInterface {

    StateKey<Integer> PAGE = StateKey.of("nucleus:paginated-menu-page", Integer.class);

    static <E> PaginatedMenuInterface<E> create(final MindustryPlugin plugin) {
        return new PaginatedMenuInterfaceImpl<>(plugin);
    }

    Supplier<Iterable<E>> getElementProvider();

    void setElementProvider(final Supplier<Iterable<E>> provider);

    Function<E, String> getElementRenderer();

    void setElementRenderer(final Function<E, String> renderer);

    BiAction<E> getChoiceAction();

    void setChoiceAction(final BiAction<E> action);

    int getPageSize();

    void setPageSize(final int size);
}
