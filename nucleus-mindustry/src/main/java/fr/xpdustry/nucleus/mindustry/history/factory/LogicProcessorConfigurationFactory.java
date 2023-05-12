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
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Text;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry.Type;
import fr.xpdustry.nucleus.mindustry.util.ImmutablePoint;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.InflaterInputStream;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class LogicProcessorConfigurationFactory extends LinkableBlockConfigurationFactory<LogicBuild> {

    private static final int MAX_INSTRUCTIONS_SIZE = 1024 * 500;

    @Override
    public Optional<HistoryConfiguration> create(
            final LogicBuild building, final Type type, final @Nullable Object config) {
        if (type == Type.PLACING || type == Type.PLACE || type == Type.BREAKING || type == Type.BREAK) {
            return getConfiguration(building);
        } else if (config instanceof byte[] bytes) {
            return readCode(bytes).map(code -> HistoryConfiguration.Text.of(code, Text.Type.CODE));
        }
        return super.create(building, type, config);
    }

    @Override
    protected boolean isLinkValid(final LogicBuild building, final int x, final int y) {
        final var link = building.links.find(l -> l.x == x && l.y == y);
        return link != null && link.active;
    }

    private Optional<HistoryConfiguration> getConfiguration(final LogicBuild building) {
        final List<HistoryConfiguration> configurations = new ArrayList<>();

        final List<ImmutablePoint> links = ArcCollections.immutableList(building.links).stream()
                .filter(link -> link.active)
                .map(link -> ImmutablePoint.of(link.x - building.tileX(), link.y - building.tileY()))
                .toList();
        if (!links.isEmpty()) {
            configurations.add(HistoryConfiguration.Link.of(links, true));
        }

        if (!building.code.isBlank()) {
            configurations.add(HistoryConfiguration.Text.of(building.code, Text.Type.CODE));
        }

        if (configurations.isEmpty()) {
            return Optional.empty();
        } else if (configurations.size() == 1) {
            return Optional.of(configurations.get(0));
        } else {
            return Optional.of(HistoryConfiguration.Composite.of(configurations));
        }
    }

    private Optional<String> readCode(final byte[] compressed) {
        try (final DataInputStream stream =
                new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(compressed)))) {
            final int version = stream.read();
            final int length = stream.readInt();

            if (length > MAX_INSTRUCTIONS_SIZE) {
                return Optional.empty();
            }

            final byte[] bytes = new byte[length];
            stream.readFully(bytes);

            final int links = stream.readInt();

            if (version == 0) {
                // old version just had links
                for (int i = 0; i < links; i++) {
                    stream.readInt();
                }
            } else {
                for (int i = 0; i < links; i++) {
                    stream.readUTF(); // name
                    stream.readShort(); // x
                    stream.readShort(); // y
                }
            }

            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (final IOException exception) {
            return Optional.empty();
        }
    }
}
