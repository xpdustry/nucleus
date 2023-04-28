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
package fr.xpdustry.nucleus.mindustry.testing.ui.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class StateImpl implements State {

    private final Map<String, Object> map = new HashMap<>();

    @SuppressWarnings({"unchecked", "NullAway"})
    @Override
    public <V> Optional<V> get(final StateKey<V> key) {
        return Optional.ofNullable((V) map.get(key.getName()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V get(final StateKey<V> key, final V def) {
        return (V) map.getOrDefault(key.getName(), def);
    }

    @Override
    public <V> State set(final StateKey<V> key, final V value) {
        this.map.put(key.getName(), value);
        return this;
    }

    @Override
    public State remove(final StateKey<?> key) {
        this.map.remove(key.getName());
        return this;
    }

    @Override
    public boolean contains(final StateKey<?> key) {
        return map.containsKey(key.getName());
    }
}
