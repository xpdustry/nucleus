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
package fr.xpdustry.nucleus.mindustry.testing.ui;

import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import java.util.function.Consumer;
import mindustry.gen.Player;

// Inspired from https://github.com/Incendo/interfaces, best interface library ever :)
public interface Interface {

    View create(final Player viewer);

    View create(final View parent);

    default View open(final Player viewer) {
        final var view = this.create(viewer);
        view.open();
        return view;
    }

    default View open(final Player viewer, final Consumer<State> consumer) {
        final var view = this.create(viewer);
        consumer.accept(view.getState());
        view.open();
        return view;
    }

    default View open(final View parent) {
        final var view = this.create(parent);
        view.open();
        return view;
    }

    default View open(final View parent, final Consumer<State> consumer) {
        final var view = this.create(parent);
        consumer.accept(view.getState());
        view.open();
        return view;
    }
}
