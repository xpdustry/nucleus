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

import java.util.HashMap;
import java.util.Map;

final class StateImpl implements State {

    private final Map<String, Object> map;

    StateImpl(final Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public <T> State with(final StateKey<T> key, final T value) {
        final var copy = copy0();
        copy.map.put(key.getName(), value);
        return copy;
    }

    @Override
    public State remove(final StateKey<?> key) {
        final var copy = copy0();
        copy.map.remove(key.getName());
        return copy;
    }

    @SuppressWarnings({"unchecked", "NullAway"})
    @Override
    public <T> T get(final StateKey<T> key) {
        return (T) map.get(key.getName());
    }

    // TODO Maybe changing the nullability annotations to Jetbrains
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final StateKey<T> key, final T def) {
        return (T) map.getOrDefault(key.getName(), def);
    }

    @Override
    public boolean contains(final StateKey<?> key) {
        return map.containsKey(key.getName());
    }

    @Override
    public State copy() {
        return copy0();
    }

    private StateImpl copy0() {
        return new StateImpl(new HashMap<>(map));
    }
}
