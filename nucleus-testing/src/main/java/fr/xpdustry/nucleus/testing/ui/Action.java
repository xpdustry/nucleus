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
package fr.xpdustry.nucleus.testing.ui;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;
import mindustry.gen.Call;

@FunctionalInterface
public interface Action<V extends View<?, ?>> extends Consumer<V> {

    static <V extends View<?, ?>> Action<V> none() {
        return view -> {};
    }

    static <V extends View<?, ?>> Action<V> open() {
        return view -> view.getInterface().open(view.getViewer(), view.getState());
    }

    static <V extends View<?, ?>, T> Action<V> openWith(final StateKey<T> key, final T value) {
        return view ->
                view.getInterface().open(view.getViewer(), view.getState().put(key, value));
    }

    static <V extends View<?, ?>, T> Action<V> openWithout(final StateKey<T> key) {
        return view ->
                view.getInterface().open(view.getViewer(), view.getState().remove(key));
    }

    static <V extends View<?, ?>> Action<V> uri(final URI uri) {
        return view -> Call.openURI(uri.toString());
    }

    @Override
    void accept(final V view);

    default Action<V> andThen(Action<? super V> after) {
        Objects.requireNonNull(after);
        return (V t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
