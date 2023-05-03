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
package fr.xpdustry.nucleus.mindustry.listener;

import arc.Core;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.distributor.api.scheduler.MindustryTimeUnit;
import fr.xpdustry.distributor.api.scheduler.TaskHandler;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.util.Pair;
import fr.xpdustry.nucleus.mindustry.util.PlayerMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import mindustry.gen.WorldLabel;
import mindustry.graphics.Layer;
import mindustry.net.Host;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class HubListener implements NucleusListener {

    private final Map<HubServerPosition, Host> servers = new HashMap<>();
    private final Map<HubServerPosition, HubServerLabel> labels = new HashMap<>();
    private final List<HubServerLabelTemplate> templates = new ArrayList<>();
    private final PlayerMap<Boolean> debug = PlayerMap.create();

    private final ConfigurationLoader<?> positionsLoader;
    private final ConfigurationLoader<?> templatesLoader;

    private final NucleusPluginCommandManager clientCommandManager;

    @Inject
    private Logger logger;

    @Inject
    public HubListener(
            final MindustryPlugin plugin, final @ClientSide NucleusPluginCommandManager clientCommandManager) {
        this.clientCommandManager = clientCommandManager;

        this.positionsLoader = YamlConfigurationLoader.builder()
                .path(plugin.getDirectory().resolve("hub-positions.yaml"))
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        this.templatesLoader = YamlConfigurationLoader.builder()
                .path(plugin.getDirectory().resolve("hub-templates.yaml"))
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .build();
    }

    @Override
    public void onNucleusInit() {
        Vars.netServer.admins.addActionFilter(action -> false);
        this.load();

        final var root = clientCommandManager.commandBuilder("hub-manager", ArgumentDescription.of("Manage the hub."));

        clientCommandManager.command(root.literal("add")
                .permission("nucleus.hub-manager.add")
                .argument(StringArgument.of("name"))
                .argument(IntegerArgument.<CommandSender>builder("x").withMin(0).build())
                .argument(IntegerArgument.<CommandSender>builder("y").withMin(0).build())
                .argument(IntegerArgument.<CommandSender>builder("size")
                        .withMin(1)
                        .withMax(Vars.maxBlockSize)
                        .build())
                .handler(ctx -> {
                    final String name = ctx.<String>get("name").toLowerCase(Locale.ROOT);
                    final int x = ctx.get("x");
                    final int y = ctx.get("y");

                    if (this.servers.keySet().stream()
                            .anyMatch(position -> position.name.equalsIgnoreCase(name) || position.contains(x, y))) {
                        ctx.getSender().sendMessage("A server with this name or position already exists.");
                        return;
                    }

                    if (x > Vars.world.width() || y > Vars.world.height()) {
                        ctx.getSender().sendMessage("The position is out of the map.");
                        return;
                    }

                    this.servers.put(new HubServerPosition(name, x, y, ctx.get("size")), null);
                    this.save();
                }));

        clientCommandManager.command(root.literal("remove")
                .permission("nucleus.hub-manager.remove")
                .argument(StringArgument.of("name"))
                .handler(ctx -> {
                    final String name = ctx.<String>get("name").toLowerCase(Locale.ROOT);
                    if (this.servers.keySet().removeIf(position -> position.name.equalsIgnoreCase(name))) {
                        this.save();
                        ctx.getSender().sendMessage("The server has been removed.");
                    } else {
                        ctx.getSender().sendMessage("No server with this name exists.");
                    }
                }));

        clientCommandManager.command(
                root.literal("reload").permission("nucleus.hub-manager.reload").handler(ctx -> this.load()));

        clientCommandManager.command(
                root.literal("debug").permission("nucleus.hub-manager.debug").handler(ctx -> {
                    this.debug.set(
                            ctx.getSender().getPlayer(),
                            !this.debug.get(ctx.getSender().getPlayer(), false));
                    if (this.debug.get(ctx.getSender().getPlayer(), false)) {
                        ctx.getSender().sendMessage("Debug mode enabled.");
                    } else {
                        ctx.getSender().sendMessage("Debug mode disabled.");
                    }
                }));
    }

    @EventHandler
    public void onPlayerTapEvent(final EventType.TapEvent event) {
        this.connect(event.player, event.tile.x, event.tile.y);
    }

    @EventHandler
    public void onPlayEvent(final EventType.PlayEvent event) {
        Vars.state.rules.tags.put("xpdustry-hub:active", "true");
        Vars.state.map.tags.put("name", "[cyan]XpdustryHub");
    }

    @TaskHandler(delay = 10L, interval = 10L, unit = MindustryTimeUnit.TICKS)
    public void onPlayerProximity() {
        if (Vars.state.isPlaying()) {
            Groups.player.forEach(player -> this.connect(player, player.tileX(), player.tileY()));
        }
    }

    @TaskHandler(interval = 10L, unit = MindustryTimeUnit.SECONDS)
    public void onServersUpdate() {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (final var position : this.servers.keySet()) {
            futures.add(this.pingHost(position.name)
                    .exceptionally(throwable -> null)
                    .thenAcceptAsync(
                            host -> {
                                this.servers.put(position, host);
                                render(position, host);
                            },
                            Core.app::post));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenRunAsync(
                        () -> Core.settings.put(
                                "totalPlayers",
                                this.servers.values().stream()
                                                .filter(Objects::nonNull)
                                                .mapToInt(host -> host.players)
                                                .sum()
                                        + Groups.player.size()),
                        Core.app::post);
    }

    private void render(final HubServerPosition position, final @Nullable Host host) {
        if (Vars.state.isGame()) {
            final var label = this.labels.computeIfAbsent(position, p -> HubServerLabel.create(p, this.templates));
            label.update(getProcessor(host));
            position.boundaries()
                    .forEach(point -> Groups.player.each(player -> debug.get(player, false), player -> {
                        Call.label(player.con(), String.valueOf(Iconc.box), 10F, point.getX(), point.getY());
                        Call.label(
                                player.con(),
                                position.name,
                                10F,
                                position.center().getX(),
                                position.center().getY());
                    }));
        } else {
            final var label = this.labels.remove(position);
            if (label != null) {
                label.remove();
            }
        }
    }

    private CompletableFuture<Host> pingHost(final String server) {
        final var future = new CompletableFuture<Host>();
        Vars.net.pingHost(server + ".md.xpdustry.fr", 6567, future::complete, future::completeExceptionally);
        return future;
    }

    private void connect(final Player player, final int x, final int y) {
        this.servers.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getKey().contains(x, y))
                .findFirst()
                .ifPresent(entry -> {
                    final var host = entry.getValue();
                    if (this.debug.get(player, false)) {
                        player.sendMessage("Connecting to " + host.address + ":" + host.port);
                    } else {
                        Call.connect(player.con(), host.address, host.port);
                        this.logger.info(
                                "Player {} connected to server {} ({}:{})",
                                player.plainName(),
                                entry.getKey().name,
                                host.address,
                                host.port);
                    }
                });
    }

    private void load() {
        this.servers.clear();

        try {
            positionsLoader.load().childrenMap().forEach((name, node) -> {
                final var x = node.node("x").getInt();
                final var y = node.node("y").getInt();
                final var size = node.node("size").getInt(4);
                this.servers.put(new HubServerPosition((String) name, x, y, size), null);
            });
            this.logger.debug("Loaded {} hub servers.", this.servers.size());
        } catch (final IOException e) {
            this.logger.error("Failed to load hub servers.", e);
        }

        this.templates.clear();
        try {
            for (final var node : templatesLoader.load().childrenList()) {
                final var text =
                        Objects.requireNonNull(node.node("text").getString()).trim();
                final var x = node.node("x").getFloat();
                final var y = node.node("y").getFloat();
                final var size = node.node("size").getFloat(2F);
                final var outline = node.node("outline").getBoolean();
                final var background = node.node("background").getBoolean();
                this.templates.add(new HubServerLabelTemplate(text, x, y, size, outline, background));
            }
            this.logger.debug("Loaded {} hub templates.", this.templates.size());
        } catch (final IOException e) {
            this.logger.error("Failed to load hub templates.", e);
        }

        this.labels.forEach((position, label) -> label.remove());
        this.labels.clear();
    }

    private void save() {
        try {
            final var node = this.positionsLoader.createNode();
            for (final var position : this.servers.keySet()) {
                node.node(position.name, "x").set(position.x);
                node.node(position.name, "y").set(position.y);
                node.node(position.name, "size").set(position.size);
            }
            this.positionsLoader.save(node);
            this.logger.debug("Saved {} hub servers.", this.servers.size());
        } catch (final IOException e) {
            this.logger.error("Failed to save hub servers.", e);
        }

        this.labels.forEach((position, label) -> label.remove());
        this.labels.clear();
    }

    private @Nullable UnaryOperator<String> getProcessor(final @Nullable Host host) {
        if (host == null) {
            return null;
        }
        return s -> s.replace("{name}", host.name)
                .replace("{players}", String.valueOf(host.players))
                .replace("{map}", host.mapname);
    }

    private List<String> getAvailableServers() {
        return this.servers.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey().name)
                .toList();
    }

    private record HubServerPosition(String name, int x, int y, int size) {
        public Position center() {
            return new Vec2(
                    (this.x * Vars.tilesize) + ((size - 1) / 2F * Vars.tilesize),
                    (this.y * Vars.tilesize) + ((size - 1) / 2F * Vars.tilesize));
        }

        public boolean contains(final int x, final int y) {
            return x > this.x && x < this.x + size && y > this.y - size && y < this.y + size;
        }

        public Position getOffsetPosition(final float x, final float y) {
            return ((Vec2) this.center()).add(x * Vars.tilesize * size, y * Vars.tilesize * size);
        }

        public List<Position> boundaries() {
            return List.of(
                    new Vec2(this.x * Vars.tilesize, this.y * Vars.tilesize),
                    new Vec2((this.x + size - 1) * Vars.tilesize, this.y * Vars.tilesize),
                    new Vec2((this.x + size - 1) * Vars.tilesize, (this.y + size - 1) * Vars.tilesize),
                    new Vec2(this.x * Vars.tilesize, (this.y + size - 1) * Vars.tilesize));
        }
    }

    private static final class HubServerLabel {

        private final WorldLabel offlineLabel;
        private final List<Pair<WorldLabel, HubServerLabelTemplate>> labels;

        private HubServerLabel(
                final WorldLabel offlineLabel, final List<Pair<WorldLabel, HubServerLabelTemplate>> labels) {
            this.offlineLabel = offlineLabel;
            this.labels = labels;
        }

        public static HubServerLabel create(
                final HubServerPosition position, final List<HubServerLabelTemplate> templates) {
            final var offlineLabel = WorldLabel.create();
            offlineLabel.text("[red]Offline");
            offlineLabel.z(Layer.flyingUnit);
            offlineLabel.flags((byte) (WorldLabel.flagOutline | WorldLabel.flagBackground));
            offlineLabel.fontSize(2F);
            offlineLabel.set(position.center());
            return new HubServerLabel(
                    offlineLabel,
                    templates.stream()
                            .map(template -> new Pair<>(template.create(position), template))
                            .toList());
        }

        public void update(final @Nullable UnaryOperator<String> interpolator) {
            if (interpolator == null) {
                this.offlineLabel.add();
                this.labels.forEach(label -> label.first().hide());
            } else {
                this.offlineLabel.hide();
                this.labels.forEach(label -> {
                    label.first().add();
                    label.first().text(interpolator.apply(label.second().text()));
                });
            }
        }

        public void remove() {
            this.offlineLabel.hide();
            this.labels.forEach(label -> label.first().hide());
        }
    }

    private record HubServerLabelTemplate(
            String text, float x, float y, float size, boolean outline, boolean background) {
        public WorldLabel create(final HubServerPosition position) {
            final var label = WorldLabel.create();
            label.text("sus");
            label.z(Layer.flyingUnit);
            label.flags((byte)
                    ((this.outline ? WorldLabel.flagOutline : 0) | (this.background ? WorldLabel.flagBackground : 0)));
            label.fontSize(size);
            label.set(position.getOffsetPosition(x, y));
            label.add();
            return label;
        }
    }
}
