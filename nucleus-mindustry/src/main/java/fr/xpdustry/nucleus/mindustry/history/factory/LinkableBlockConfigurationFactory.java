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

import arc.math.geom.Point2;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry.Type;
import fr.xpdustry.nucleus.mindustry.util.ImmutablePoint;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import mindustry.gen.Building;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class LinkableBlockConfigurationFactory<B extends Building> implements HistoryConfiguration.Factory<B> {

    @Override
    public Optional<HistoryConfiguration> create(final B building, final Type type, final @Nullable Object config) {
        if (config == null || !building.block().configurations.containsKey(config.getClass())) {
            return Optional.empty();
        }

        if (config instanceof Integer integer) {
            if (integer == -1 || integer == building.pos()) {
                return Optional.of(HistoryConfiguration.Link.reset());
            }
            final var point = Point2.unpack(integer);
            if (point.x < 0 || point.y < 0) {
                return Optional.empty();
            }
            return Optional.of(HistoryConfiguration.Link.of(
                    List.of(ImmutablePoint.of(point.x - building.tileX(), point.y - building.tileY())),
                    this.isLinkValid(building, point.x, point.y)));
        } else if (config instanceof Point2 point) {
            // Point2 are used by schematics, so they are already relative to the building
            return Optional.of(HistoryConfiguration.Link.of(
                    List.of(ImmutablePoint.of(point.x, point.y)),
                    this.isLinkValid(building, point.x + building.tileX(), point.y + building.tileY())));
        } else if (config instanceof Point2[] array) {
            final var links = new ImmutablePoint[array.length];
            for (int i = 0; i < array.length; i++) {
                final var point2 = array[i];
                links[i] = ImmutablePoint.of(point2.x, point2.y);
            }
            return Optional.of(HistoryConfiguration.Link.of(Arrays.asList(links), true));
        } else {
            return Optional.empty();
        }
    }

    protected abstract boolean isLinkValid(final B building, final int x, final int y);
}
