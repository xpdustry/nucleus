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
package fr.xpdustry.nucleus.mindustry.util;

import arc.Events;
import java.util.HashMap;
import java.util.Map;
import mindustry.game.EventType;
import mindustry.gen.Player;

// TODO Delegate the creation to a factory
public final class PlayerMap<V> {

    private final Map<String, V> players = new HashMap<>();

    {
        // TODO Migrate to MoreEvents
        Events.on(EventType.PlayerLeave.class, event -> players.remove(event.player.uuid()));
    }

    public static <V> PlayerMap<V> create() {
        return new PlayerMap<>();
    }

    private PlayerMap() {}

    public V get(final Player player, final V def) {
        return players.getOrDefault(player.uuid(), def);
    }

    public void set(final Player player, final V value) {
        players.put(player.uuid(), value);
    }
}
