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
import org.checkerframework.checker.nullness.qual.*;

final class StateImpl implements State {

    private final Map<String, Object> map;

    StateImpl(final Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public <T> State put(final StateKey<T> key, final T value) {
        map.put(key.getName(), value);
        return this;
    }

    @Override
    public State remove(StateKey<?> key) {
        map.remove(key.getName());
        return this;
    }

    @SuppressWarnings({"unchecked", "NullAway"})
    @Override
    public <T> @PolyNull T get(final StateKey<T> key) {
        return (T) map.get(key.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final StateKey<T> key, final T def) {
        return (T) map.getOrDefault(key.getName(), def);
    }

    @Override
    public boolean has(final StateKey<?> key) {
        return map.containsKey(key.getName());
    }

    @Override
    public State copy() {
        return new StateImpl(new HashMap<>(map));
    }
}
