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
package fr.xpdustry.nucleus.mindustry.history;

import java.util.List;
import mindustry.gen.Building;
import mindustry.gen.Player;
import mindustry.world.Tile;

public interface HistoryService {

    <B extends Building> void setConfigurationFactory(
            final Class<B> clazz, final HistoryConfiguration.Factory<B> factory);

    List<HistoryEntry> getHistory(final String uuid);

    List<HistoryEntry> getHistory(final Tile tile);

    default List<HistoryEntry> getHistory(final Player player) {
        return this.getHistory(player.uuid());
    }
}
