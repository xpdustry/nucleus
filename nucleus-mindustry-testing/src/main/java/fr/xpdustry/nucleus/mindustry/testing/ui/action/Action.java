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
package fr.xpdustry.nucleus.mindustry.testing.ui.action;

import fr.xpdustry.nucleus.mindustry.testing.ui.View;
import fr.xpdustry.nucleus.mindustry.testing.ui.state.State;
import java.net.URI;
import java.util.function.Consumer;
import mindustry.Vars;
import mindustry.gen.Call;

@FunctionalInterface
public interface Action {

    static Action none() {
        return view -> {};
    }

    static Action close() {
        return view -> {
            view.close();
            view.getParent().ifPresent(parent -> Action.open().accept(parent));
        };
    }

    static Action closeAll() {
        return view -> {
            var current = view;
            while (current != null) {
                current.close();
                current = current.getParent().orElse(null);
            }
        };
    }

    static Action open() {
        return View::open;
    }

    static Action open(final Consumer<State> consumer) {
        return view -> {
            consumer.accept(view.getState());
            view.open();
        };
    }

    static Action uri(final URI uri) {
        return view -> Call.openURI(uri.toString());
    }

    static Action run(final Runnable runnable) {
        return view -> runnable.run();
    }

    static Action command(final String name, final String... arguments) {
        final var builder = new StringBuilder(name.length() + 1 + (arguments.length * 4));
        builder.append('/').append(name);
        for (final var argument : arguments) {
            builder.append(' ').append(argument);
        }
        final var input = builder.toString();
        return view -> Vars.netServer.clientCommands.handleMessage(input, view.getViewer());
    }

    void accept(final View view);

    default Action then(final Action after) {
        return view -> {
            this.accept(view);
            after.accept(view);
        };
    }

    default <T> BiAction<T> asBiAction() {
        return (view, value) -> this.accept(view);
    }
}
