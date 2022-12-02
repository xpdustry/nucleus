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
package fr.xpdustry.nucleus.mindustry.action;

import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.util.CommandHandler;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.util.MoreEvents;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.io.Serial;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.distribution.DuctRouter;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.MassDriver;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.payloads.PayloadMassDriver;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.blocks.sandbox.ItemSource;
import mindustry.world.blocks.sandbox.LiquidSource;
import mindustry.world.blocks.units.UnitFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

// https://github.com/Pointifix/HistoryPlugin
public final class BlockInspector implements PluginListener {

    private final Set<String> inspectors = new HashSet<>();
    private final IntMap<LimitedList<PlayerAction>> data = new IntMap<>();
    private final NucleusPlugin nucleus;

    public BlockInspector(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public void onPluginInit() {
        MoreEvents.subscribe(EventType.WorldLoadEvent.class, event -> {
            this.data.clear();
        });

        MoreEvents.subscribe(EventType.PlayerLeave.class, event -> {
            this.inspectors.remove(event.player.uuid());
        });

        MoreEvents.subscribe(EventType.BlockBuildEndEvent.class, event -> {
            if (!event.unit.isPlayer()) {
                return;
            }

            final var block = event.breaking && event.tile.build != null
                    ? ((ConstructBlock.ConstructBuild) event.tile.build).current
                    : event.tile.block();

            this.getLinkedTiles(event.tile, block, tile -> this.getTileActions(tile)
                    .add(ImmutableBlockAction.builder()
                            .author(event.unit.getPlayer().uuid())
                            .type(event.breaking ? BlockAction.Type.BREAK : BlockAction.Type.PLACE)
                            .block(block)
                            .build()));
        });

        MoreEvents.subscribe(EventType.ConfigEvent.class, event -> {
            if (event.player == null) {
                return;
            }
            // For some reason, bridges are set 2 times when disconnecting them,
            // it fills the history with unnecessary data
            this.getLinkedTiles(event.tile.tile(), event.tile.block(), linked -> this.getTileActions(linked)
                    .add(ImmutableConfigAction.builder()
                            .author(event.player.uuid())
                            .block(event.tile.tile().block())
                            .config(event.value)
                            .connect(isLinkableBlock(event.tile.block(), event.value)
                                    && isLinked(event.tile, event.value))
                            .build()));
        });

        MoreEvents.subscribe(EventType.TapEvent.class, event -> {
            if (this.inspectors.contains(event.player.uuid())) {
                if (!this.data.containsKey(event.tile.pos())
                        || this.getTileActions(event.tile).isEmpty()) {
                    event.player.sendMessage("No data for this tile");
                    return;
                }

                final var builder = new StringBuilder()
                        .append("[yellow]History of Block (")
                        .append(event.tile.x)
                        .append(",")
                        .append(event.tile.y)
                        .append(")");

                final var iterator = getTileActions(event.tile).descendingIterator();
                while (iterator.hasNext()) {
                    final var action = iterator.next();

                    builder.append("\n[white]> ").append(Vars.netServer.admins.getInfo(action.getAuthor()).lastName);

                    if (event.player.admin()) {
                        builder.append(" [lightgray](UUID:")
                                .append(action.getAuthor())
                                .append(")");
                    }
                    builder.append("[white] ");

                    if (action instanceof BlockAction block) {
                        if (block.getType() == BlockAction.Type.BREAK) {
                            builder.append("[red]broke[] this tile");
                        } else {
                            builder.append("placed [accent]").append(block.getBlock().name);
                        }
                    } else if (action instanceof ConfigAction config) {
                        final var components = toStringComponents(config, event.tile.pos());
                        builder.append("[accent]").append(components[0]).append("[] this tile");
                        if (components.length > 1) {
                            builder.append(' ')
                                    .append(components.length != 3 ? "to" : components[2])
                                    .append(" [green]")
                                    .append(components[1]);
                        }
                    }
                }

                event.player.sendMessage(builder.toString());
            }
        });
    }

    @Override
    public void onPluginClientCommandsRegistration(final CommandHandler handler) {
        final var manager = this.nucleus.getClientCommands();
        manager.command(manager.commandBuilder("inspector")
                .meta(CommandMeta.DESCRIPTION, "Toggle inspector mode.")
                .handler(ctx -> {
                    if (this.inspectors.add(ctx.getSender().getPlayer().uuid())) {
                        ctx.getSender().sendMessage("Inspector mode enabled.");
                    } else {
                        this.inspectors.remove(ctx.getSender().getPlayer().uuid());
                        ctx.getSender().sendMessage("Inspector mode disabled.");
                    }
                }));
    }

    private LimitedList<PlayerAction> getTileActions(final Tile tile) {
        return this.data.get(
                tile.pos(),
                () -> new LimitedList<>(this.nucleus.getConfiguration().getInspectorHistoryLimit()));
    }

    // Returns an array decomposing the action, first element is the verb, second the config, third the target
    private String[] toStringComponents(final ConfigAction action, final int pos) {
        if (this.isLinkableBlock(action.getBlock(), action.getConfig())
                && (action.getConfig() == null || action.getConfig() instanceof Integer)) {
            if (action.getConnect()) {
                if (action.getConfig() == null || (int) action.getConfig() < 0 || (int) action.getConfig() == pos) {
                    return new String[] {"disconnected"};
                }
                final var point = Point2.unpack((int) action.getConfig());
                return new String[] {"connected", point.x + "," + point.y};
            } else {
                if (action.getConfig() == null || (int) action.getConfig() < 0) {
                    return new String[] {"disconnected"};
                }
                final var point = Point2.unpack((int) action.getConfig());
                return new String[] {"disconnected", point.x + "," + point.y, "from"};
            }
        } else if (this.isItemConfigurableBlock(action.getBlock())) {
            if (action.getConfig() == null) {
                return new String[] {"configured", "default"};
            }
            return new String[] {"configured", action.getConfig().toString()};
        } else if (action.getBlock() instanceof UnitFactory factory) {
            if (action.getConfig() == null || (int) action.getConfig() < 0) {
                return new String[] {"configured", "default"};
            }
            return new String[] {"configured", factory.plans.get((int) action.getConfig()).unit.name};
        } else {
            return new String[] {"configured", Objects.toString(action.getConfig())};
        }
    }

    private boolean isLinkableBlock(final Block block, final @Nullable Object config) {
        if (config == null) {
            return false;
        }
        return (block instanceof LogicBlock && config instanceof Integer)
                || block instanceof PowerNode
                || block instanceof MassDriver
                || block instanceof PayloadMassDriver
                || block instanceof ItemBridge;
    }

    private boolean isLinked(final Building building, final Object config) {
        if (config instanceof Point2[]) {
            return true;
        }
        final var pos = config instanceof Point2 point ? point.pack() : (int) config;
        if (building instanceof LogicBlock.LogicBuild build) {
            final var link = build.links.find(l -> Point2.pack(l.x, l.y) == pos);
            return link != null && link.active;
        } else if (building instanceof PowerNode.PowerNodeBuild build) {
            return build.power().links.contains(pos);
        } else if (building instanceof MassDriver.MassDriverBuild build) {
            return build.link == pos;
        } else if (building instanceof PayloadMassDriver.PayloadDriverBuild build) {
            return build.link == pos;
        } else if (building instanceof ItemBridge.ItemBridgeBuild build) {
            return build.link == pos;
        } else {
            return false;
        }
    }

    private boolean isItemConfigurableBlock(final Block block) {
        return block instanceof Sorter
                || block instanceof ItemSource
                || block instanceof LiquidSource
                || block instanceof DuctRouter;
    }

    private void getLinkedTiles(final Tile tile, final Block block, final Consumer<Tile> consumer) {
        if (block.isMultiblock()) {
            int size = block.size, offset = block.sizeOffset;
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    final var other = Vars.world.tile(tile.x + dx + offset, tile.y + dy + offset);
                    if (other != null) {
                        consumer.accept(other);
                    }
                }
            }
        } else {
            consumer.accept(tile);
        }
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
