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

import fr.xpdustry.distributor.api.util.ArcCollections;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry.Type;
import java.util.Optional;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CommonConfigurationFactory implements HistoryConfiguration.Factory<Building> {

    @Override
    public Optional<HistoryConfiguration> create(
            final Building building, final Type type, final @Nullable Object config) {
        if (isContentConfigurableBlockOnly(building)) {
            if (config == null) {
                return Optional.of(HistoryConfiguration.Content.empty());
            } else if (config instanceof UnlockableContent content) {
                return Optional.of(HistoryConfiguration.Content.of(content));
            }
        } else if (isEnablingBlockOnly(building)) {
            if (config instanceof Boolean enabled) {
                return Optional.of(HistoryConfiguration.Enable.of(enabled));
            }
        }
        return Optional.empty();
    }

    private boolean isContentConfigurableBlockOnly(final Building building) {
        for (final var configuration : building.block().configurations.keys()) {
            if (!(UnlockableContent.class.isAssignableFrom(configuration) || configuration == Void.TYPE)) {
                return false;
            }
        }
        return true;
    }

    private boolean isEnablingBlockOnly(final Building building) {
        final var keys =
                ArcCollections.immutableMap(building.block().configurations).keySet();
        return keys.size() == 1 && keys.contains(Boolean.class);
    }
}
