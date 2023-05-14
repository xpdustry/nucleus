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
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Simple;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry.Type;
import fr.xpdustry.nucleus.mindustry.history.factory.CanvasConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.CommonConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.ItemBridgeConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.LightBlockConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.LogicProcessorConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.MassDriverConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.MessageBlockConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.PayloadMassDriverConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.PowerNodeConfigurationFactory;
import fr.xpdustry.nucleus.mindustry.history.factory.UnitFactoryConfigurationFactory;
import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild;
import mindustry.world.blocks.distribution.MassDriver.MassDriverBuild;
import mindustry.world.blocks.logic.CanvasBlock.CanvasBuild;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.logic.MessageBlock.MessageBuild;
import mindustry.world.blocks.payloads.PayloadMassDriver.PayloadDriverBuild;
import mindustry.world.blocks.power.LightBlock.LightBuild;
import mindustry.world.blocks.power.PowerNode.PowerNodeBuild;
import mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild;
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

        // TODO I don't like to have this much classes here, maybe we can find a way to avoid this
        this.setConfigurationFactory(CanvasBuild.class, new CanvasConfigurationFactory());
        this.setConfigurationFactory(Building.class, new CommonConfigurationFactory());
        this.setConfigurationFactory(ItemBridgeBuild.class, new ItemBridgeConfigurationFactory());
        this.setConfigurationFactory(LightBuild.class, new LightBlockConfigurationFactory());
        this.setConfigurationFactory(LogicBuild.class, new LogicProcessorConfigurationFactory());
        this.setConfigurationFactory(MassDriverBuild.class, new MassDriverConfigurationFactory());
        this.setConfigurationFactory(MessageBuild.class, new MessageBlockConfigurationFactory());
        this.setConfigurationFactory(PayloadDriverBuild.class, new PayloadMassDriverConfigurationFactory());
        this.setConfigurationFactory(PowerNodeBuild.class, new PowerNodeConfigurationFactory());
        this.setConfigurationFactory(UnitFactoryBuild.class, new UnitFactoryConfigurationFactory());
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
        if (event.unit == null || event.tile.build == null) {
            return;
        }

        final var block =
                event.breaking ? ((ConstructBlock.ConstructBuild) event.tile.build).current : event.tile.block();

        this.addEntry(event.tile.build, block, event.unit, event.breaking ? Type.BREAK : Type.PLACE, event.config);
    }

    @EventHandler
    public void onBlockDestroyBeginEvent(final EventType.BlockBuildBeginEvent event) {
        if (event.unit == null || !(event.tile.build instanceof ConstructBlock.ConstructBuild build)) {
            return;
        }
        this.addEntry(
                build, build.current, event.unit, event.breaking ? Type.BREAKING : Type.PLACING, build.lastConfig);
    }

    @EventHandler
    public void onBLockConfigEvent(final EventType.ConfigEvent event) {
        if (event.player == null) {
            return;
        }
        this.addEntry(event.tile, event.tile.block(), event.player.unit(), Type.CONFIGURE, event.value);
    }

    @EventHandler
    public void onWorldLoadEvent(final EventType.WorldLoadEvent event) {
        this.positions.clear();
        this.players.clear();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Optional<HistoryConfiguration> getConfiguration(
            final Building building, final HistoryEntry.Type type, final @Nullable Object config) {
        if (building.block().configurations.isEmpty()) {
            return Optional.empty();
        }

        Class<?> clazz = building.getClass();
        while (Building.class.isAssignableFrom(clazz)) {
            final HistoryConfiguration.Factory factory = factories.get(clazz);
            if (factory != null) {
                return factory.create(building, type, config);
            }
            clazz = clazz.getSuperclass();
        }

        return Optional.of(config == null ? Simple.empty() : Simple.of(config));
    }

    private void addEntry(
            final Building building,
            final Block block,
            final Unit unit,
            final Type type,
            final @Nullable Object config) {
        final var configuration = this.getConfiguration(building, type, config);
        final var author = HistoryAuthor.of(unit);
        building.tile.getLinkedTiles(t -> this.addEntry(HistoryEntry.builder()
                .setX(t.x)
                .setBuildX(building.tileX())
                .setY(t.y)
                .setBuildY(building.tileY())
                .setAuthor(author)
                .setBlock(block)
                .setVirtual(t.pos() != building.tile.pos())
                .setConfiguration(configuration)
                .setType(type)
                .build()));
    }

    private void addEntry(final HistoryEntry entry) {
        final var entries = this.positions.computeIfAbsent(
                Point2.pack(entry.getX(), entry.getY()),
                position -> new LimitedList<>(configuration.getHistoryTileLimit()));

        final var previous = entries.peekLast();
        // Some blocks have repeating configurations, we don't want to spam the history with them
        if (previous != null && this.haveSameConfiguration(previous, entry)) {
            entries.removeLast();
        }
        entries.add(entry);

        if (entry.getAuthor().getUuid().isPresent() && !entry.isVirtual())
            this.players
                    .computeIfAbsent(
                            entry.getAuthor().getUuid().get(),
                            player -> new LimitedList<>(configuration.getHistoryPlayerLimit()))
                    .add(entry);
    }

    private boolean haveSameConfiguration(final HistoryEntry entryA, final HistoryEntry entryB) {
        return entryA.getBlock().equals(entryB.getBlock())
                && entryA.getConfiguration().equals(entryB.getConfiguration())
                && entryA.getType() == entryB.getType();
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
