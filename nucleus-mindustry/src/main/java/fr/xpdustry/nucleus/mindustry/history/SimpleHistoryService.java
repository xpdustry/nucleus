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

import arc.math.geom.Point2;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.mindustry.NucleusPluginConfiguration;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Factory;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry.Type;
import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import org.checkerframework.checker.nullness.qual.Nullable;

// TODO Hide this class since the event listeners are exposed
public final class SimpleHistoryService implements HistoryService, NucleusListener {

    private final Map<Integer, LimitedList<HistoryEntry>> positions = new HashMap<>();
    private final Map<String, LimitedList<HistoryEntry>> players = new HashMap<>();
    private final Map<Class<? extends Building>, Factory<?>> factories = new HashMap<>();
    private final NucleusPluginConfiguration configuration;

    @Inject
    public SimpleHistoryService(final NucleusPluginConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <B extends Building> void setConfigurationFactory(final Class<B> clazz, final Factory<B> factory) {
        this.factories.put(clazz, factory);
    }

    @Override
    public List<HistoryEntry> getHistory(final int x, final int y) {
        final var history = this.positions.get(Point2.pack(x, y));
        return history == null ? Collections.emptyList() : Collections.unmodifiableList(history);
    }

    @Override
    public List<HistoryEntry> getHistory(final String uuid) {
        final var history = this.players.get(uuid);
        return history == null ? Collections.emptyList() : Collections.unmodifiableList(history);
    }

    @EventHandler
    public void onBlockBuildEndEvent(final EventType.BlockBuildEndEvent event) {
        if (event.unit == null) {
            return;
        }

        final var block = event.breaking && event.tile.build != null
                ? ((ConstructBlock.ConstructBuild) event.tile.build).current
                : event.tile.block();

        this.addBuildEntry(event.tile, event.unit, block, event.breaking ? Type.BREAK : Type.PLACE);
        if (event.config != null) {
            this.addConfigEntry(event.unit, event.tile, event.config);
        }
    }

    @EventHandler
    public void onBlockDestroyBeginEvent(final EventType.BlockBuildBeginEvent event) {
        if (event.unit == null || !(event.tile.build instanceof ConstructBlock.ConstructBuild build)) {
            return;
        }
        this.addBuildEntry(event.tile, event.unit, build.current, event.breaking ? Type.BREAKING : Type.PLACING);
    }

    @EventHandler
    public void onBLockConfigEvent(final EventType.ConfigEvent event) {
        if (event.player == null) {
            return;
        }
        this.addConfigEntry(event.player.unit(), event.tile.tile, event.value);
    }

    @EventHandler
    public void onWorldLoadEvent(final EventType.WorldLoadEvent event) {
        this.positions.clear();
        this.players.clear();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private HistoryConfiguration getConfiguration(final Building building, final @Nullable Object config) {
        Class<?> clazz = building.getClass();
        while (Building.class.isAssignableFrom(clazz)) {
            final HistoryConfiguration.Factory factory = factories.get(clazz);
            if (factory != null) {
                return factory.create(building, config);
            }
            clazz = clazz.getSuperclass();
        }
        return config == null ? HistoryConfiguration.Unknown.empty() : HistoryConfiguration.Unknown.of(config);
    }

    private void addConfigEntry(final Unit unit, final Tile tile, final @Nullable Object config) {
        if (tile.build == null || tile.build.block().configurations.isEmpty()) {
            return;
        }
        final var configuration = this.getConfiguration(tile.build, config);
        final var author = HistoryAuthor.of(unit);
        tile.getLinkedTiles(t -> this.addEntry(HistoryEntry.builder()
                .setX(t.x)
                .setY(t.y)
                .setAuthor(author)
                .setBlock(tile.build.block())
                .setConfiguration(configuration)
                .setType(Type.CONFIGURE)
                .setVirtual(t.pos() != tile.pos())
                .build()));
    }

    private void addBuildEntry(final Tile tile, final Unit unit, final Block block, final HistoryEntry.Type type) {
        if (type == Type.CONFIGURE) {
            throw new IllegalStateException("Cannot add a build entry with a configuration type");
        }
        final var author = HistoryAuthor.of(unit);
        tile.getLinkedTiles(t -> this.addEntry(HistoryEntry.builder()
                .setX(t.x)
                .setY(t.y)
                .setAuthor(author)
                .setBlock(block)
                .setVirtual(t.pos() != tile.pos())
                .setType(type)
                .build()));
    }

    private void addEntry(final HistoryEntry entry) {
        this.positions
                .computeIfAbsent(
                        Point2.pack(entry.getX(), entry.getY()),
                        position -> new LimitedList<>(configuration.getHistoryTileLimit()))
                .add(entry);
        if (entry.getAuthor().isPlayer() && !entry.isVirtual())
            this.players
                    .computeIfAbsent(
                            entry.getAuthor().getUuid().orElseThrow(),
                            player -> new LimitedList<>(configuration.getHistoryPlayerLimit()))
                    .add(entry);
    }

    @SuppressWarnings("JdkObsolete")
    private static final class LimitedList<E> extends LinkedList<E> {

        @Serial
        private static final long serialVersionUID = -6315128929800347513L;

        private final int limit;

        public LimitedList(final int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(final E e) {
            if (this.size() >= this.limit) {
                this.removeFirst();
            }
            return super.add(e);
        }
    }
}
