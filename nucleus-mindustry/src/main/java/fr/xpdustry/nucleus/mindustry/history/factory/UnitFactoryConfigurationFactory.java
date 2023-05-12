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
import java.util.Optional;
import mindustry.type.UnitType;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class UnitFactoryConfigurationFactory implements HistoryConfiguration.Factory<UnitFactoryBuild> {

    @Override
    public Optional<HistoryConfiguration> create(
            final UnitFactoryBuild building, final Type type, final @Nullable Object config) {
        final var plans = ((UnitFactory) building.block).plans;
        if (config instanceof Integer integer) {
            return integer > 0 && integer < plans.size
                    ? Optional.of(HistoryConfiguration.Content.of(plans.get(integer).unit))
                    : Optional.of(HistoryConfiguration.Content.empty());
        } else if (config instanceof UnitType unit) {
            return create(building, type, plans.indexOf(plan -> plan.unit.equals(unit)));
        }
        return Optional.empty();
    }
}
