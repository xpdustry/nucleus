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
package fr.xpdustry.nucleus.mindustry.history.factory;

import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry.Type;
import java.awt.Color;
import java.util.Optional;
import mindustry.world.blocks.power.LightBlock.LightBuild;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class LightBlockConfigurationFactory implements HistoryConfiguration.Factory<LightBuild> {

    @Override
    public Optional<HistoryConfiguration> create(LightBuild building, Type type, @Nullable Object config) {
        if (config instanceof Integer color) {
            return Optional.of(HistoryConfiguration.Color.of(new Color(color, true)));
        }
        return Optional.empty();
    }
}
