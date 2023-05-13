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
package fr.xpdustry.nucleus.mindustry.commands;

import arc.graphics.Colors;
import arc.util.Strings;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.argument.PlayerInfoArgument;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.annotation.ServerSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.history.HistoryAuthor;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Canvas;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Color;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Composite;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Content;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Enable;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Link;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Simple;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Text;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry.Type;
import fr.xpdustry.nucleus.mindustry.history.HistoryService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.net.Administration.PlayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO Add interactive mode like the "/inspector" command ?
public final class HistoryCommand implements NucleusListener {

    private static final Logger logger = LoggerFactory.getLogger(HistoryCommand.class);

    static {
        // TODO PR to load Vars.ui colors in server ?
        Colors.put("accent", Pal.accent);
    }

    private final HistoryService history;
    private final NucleusPluginCommandManager clientCommandManager;
    private final NucleusPluginCommandManager serverCommandManager;

    @Inject
    public HistoryCommand(
            final HistoryService history,
            final @ClientSide NucleusPluginCommandManager clientCommandManager,
            final @ServerSide NucleusPluginCommandManager serverCommandManager) {
        this.history = history;
        this.clientCommandManager = clientCommandManager;
        this.serverCommandManager = serverCommandManager;
    }

    @Override
    public void onNucleusInit() {
        this.withCommandManagers(manager -> manager.command(manager.commandBuilder("history")
                .literal("player")
                .meta(CommandMeta.DESCRIPTION, "Show the history of a player.")
                .argument(PlayerInfoArgument.of("player"))
                .argument(IntegerArgument.<CommandSender>builder("limit")
                        .withMin(1)
                        .withMax(50)
                        .asOptionalWithDefault(10))
                .handler(ctx -> {
                    final PlayerInfo info = ctx.get("player");
                    final var entries = normalize(history.getHistory(info.id), ctx.get("limit"));

                    if (entries.isEmpty()) {
                        ctx.getSender().sendWarning("No history found.");
                        return;
                    }

                    final var builder =
                            new StringBuilder("[accent]History of player [white]").append(info.plainLastName());
                    if (this.canSeeUuid(ctx.getSender())) {
                        builder.append(" [accent](").append(info.id).append(")");
                    }
                    builder.append(":");

                    for (final var entry : entries) {
                        builder.append("\n[accent] > ").append(renderEntry(entry, false, false, true, 3));
                    }

                    // TODO I really need this Component API
                    ctx.getSender()
                            .sendMessage(
                                    ctx.getSender().isConsole()
                                            ? Strings.stripColors(builder.toString())
                                            : builder.toString());
                })));

        this.withCommandManagers(manager -> manager.command(manager.commandBuilder("history")
                .literal("tile")
                .meta(CommandMeta.DESCRIPTION, "Show the history of a tile.")
                .argument(IntegerArgument.<CommandSender>builder("x").withMin(0).withMax(Short.MAX_VALUE))
                .argument(IntegerArgument.<CommandSender>builder("y").withMin(0).withMax(Short.MAX_VALUE))
                .argument(IntegerArgument.<CommandSender>builder("limit")
                        .withMin(1)
                        .withMax(50)
                        .asOptionalWithDefault(10))
                .handler(ctx -> {
                    final int x = ctx.get("x");
                    final int y = ctx.get("y");
                    final var entries = normalize(history.getHistory(x, y), ctx.get("limit"));

                    if (entries.isEmpty()) {
                        ctx.getSender().sendWarning("No history found.");
                        return;
                    }

                    final var builder = new StringBuilder("[accent]History of tile [white]")
                            .append("(")
                            .append(x)
                            .append(", ")
                            .append(y)
                            .append(")[]:");

                    for (final var entry : entries) {
                        builder.append("\n[accent] > ")
                                .append(renderEntry(entry, true, this.canSeeUuid(ctx.getSender()), false, 3));
                    }

                    // TODO I really need this Component API
                    ctx.getSender()
                            .sendMessage(
                                    ctx.getSender().isConsole()
                                            ? Strings.stripColors(builder.toString())
                                            : builder.toString());
                })));
    }

    private String renderEntry(
            final HistoryEntry entry, final boolean name, final boolean uuid, final boolean position, final int ident) {
        final var builder = new StringBuilder("[white]");

        if (name) {
            builder.append(getName(entry.getAuthor()));
            if (uuid && entry.getAuthor().getUuid().isPresent()) {
                builder.append(" [gray](")
                        .append(entry.getAuthor().getUuid().get())
                        .append(")");
            }
            builder.append("[white]: ");
        }

        // TODO Add red color to BREAK and BREAKING
        switch (entry.getType()) {
            case PLACING -> builder.append("Began construction of [accent]").append(entry.getBlock().name);
            case PLACE -> builder.append("Constructed [accent]").append(entry.getBlock().name);
            case BREAKING -> builder.append("Began deconstruction of [accent]").append(entry.getBlock().name);
            case BREAK -> builder.append("Deconstructed [accent]").append(entry.getBlock().name);
            case CONFIGURE -> renderConfiguration(
                    builder, entry, entry.getConfiguration().orElseThrow(), ident);
        }

        if (entry.getType() != Type.CONFIGURE && entry.getConfiguration().isPresent()) {
            renderConfiguration(
                    builder.append(" ".repeat(ident)).append("\n[accent] > [white]"),
                    entry,
                    entry.getConfiguration().get(),
                    ident + 3);
        }

        builder.append("[white]");
        if (position) {
            builder.append(" at [accent](")
                    .append(entry.getX())
                    .append(", ")
                    .append(entry.getY())
                    .append(")");
        }

        return builder.toString();
    }

    private void renderConfiguration(
            final StringBuilder builder,
            final HistoryEntry entry,
            final HistoryConfiguration configuration,
            final int ident) {
        if (configuration instanceof Composite composite) {
            builder.append("Configured [accent]").append(entry.getBlock().name).append("[white]:");
            for (final var component : composite.getConfigurations()) {
                renderConfiguration(
                        builder.append("\n").append(" ".repeat(ident)).append("[accent] - [white]"),
                        entry,
                        component,
                        ident + 3);
            }
        } else if (configuration instanceof Text text) {
            builder.append("Changed the [accent]")
                    .append(text.getType().name().toLowerCase(Locale.ROOT))
                    .append("[white] of [accent]")
                    .append(entry.getBlock().name);
            if (text.getType() == Text.Type.MESSAGE) {
                builder.append("[white] to [gray]").append(text.getText());
            }
        } else if (configuration instanceof Link link) {
            if (link.getType() == Link.Type.RESET) {
                builder.append("Reset the links of [accent]").append(entry.getBlock().name);
                return;
            }
            builder.append(link.getType() == Link.Type.CONNECT ? "Connected" : "Disconnected")
                    .append(" [accent]")
                    .append(entry.getBlock().name)
                    .append("[white] ")
                    .append(link.getType() == Link.Type.CONNECT ? "to" : "from")
                    .append(" [accent]")
                    .append(link.getPositions().stream()
                            .map(point -> "(" + (point.getX() + entry.getBuildX()) + ", "
                                    + (point.getY() + entry.getBuildY()) + ")")
                            .collect(Collectors.joining(", ")));
        } else if (configuration instanceof Enable enable) {
            builder.append(enable.getValue() ? "Enabled" : "Disabled")
                    .append(" [accent]")
                    .append(entry.getBlock().name);
        } else if (configuration instanceof Content content) {
            if (content.getValue().isEmpty()) {
                builder.append("Reset the content of [accent]").append(entry.getBlock().name);
                return;
            }
            builder.append("Configured [accent]")
                    .append(entry.getBlock().name)
                    .append("[white] to [accent]")
                    .append(content.getValue().orElseThrow().name);
        } else if (configuration instanceof Color color) {
            builder.append("Configured [accent]")
                    .append(entry.getBlock().name)
                    .append("[white] to [accent]")
                    .append(toHex(color.getColor()));
        } else if (configuration instanceof Canvas canvas) {
            builder.append("Changed the content of [accent]").append(entry.getBlock().name);
        } else if (configuration instanceof Simple simple) {
            builder.append("Configured [accent]")
                    .append(entry.getBlock().name)
                    .append("[white] to [accent]")
                    .append(simple.getValue().map(Object::toString).orElse("null"));
        } else {
            logger.warn(
                    "Unhandled configuration type: {}", configuration.getClass().getName());
            builder.append("Configured [accent]")
                    .append(entry.getBlock().name)
                    .append("[white] to [accent]")
                    .append(configuration);
        }
    }

    private String getName(final HistoryAuthor author) {
        return author.getUuid().isPresent()
                ? Vars.netServer.admins.getInfo(author.getUuid().get()).lastName
                : author.getTeam().name.toLowerCase(Locale.ROOT) + " " + author.getUnit().name;
    }

    private void withCommandManagers(final Consumer<NucleusPluginCommandManager> consumer) {
        consumer.accept(this.clientCommandManager);
        consumer.accept(this.serverCommandManager);
    }

    private boolean canSeeUuid(final CommandSender sender) {
        return sender.isConsole() || sender.getPlayer().admin();
    }

    private List<HistoryEntry> normalize(final List<HistoryEntry> entries, final int limit) {
        // First we sort by timestamp from latest to earliest, then we take the first N elements,
        // then we reverse the list so the latest entries are at the end
        return entries.stream()
                .sorted(Comparator.comparing(HistoryEntry::getTimestamp).reversed())
                .limit(limit)
                .sorted(Comparator.comparing(HistoryEntry::getTimestamp))
                .toList();
    }

    private String toHex(final java.awt.Color color) {
        // https://stackoverflow.com/a/3607942
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
