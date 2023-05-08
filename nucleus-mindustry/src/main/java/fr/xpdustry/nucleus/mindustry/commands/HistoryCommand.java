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
import fr.xpdustry.nucleus.common.inject.EnableScanning;
import fr.xpdustry.nucleus.mindustry.annotation.ClientSide;
import fr.xpdustry.nucleus.mindustry.annotation.ServerSide;
import fr.xpdustry.nucleus.mindustry.command.NucleusPluginCommandManager;
import fr.xpdustry.nucleus.mindustry.history.HistoryAuthor;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration;
import fr.xpdustry.nucleus.mindustry.history.HistoryEntry;
import fr.xpdustry.nucleus.mindustry.history.HistoryService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.net.Administration.PlayerInfo;
import mindustry.world.Block;

// TODO Add interactive mode like the "/inspector" command ?
@EnableScanning
public final class HistoryCommand implements NucleusListener {

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
                        builder.append("\n[accent] > ").append(renderEntry(entry, false, false, true));
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
                                .append(renderEntry(entry, true, this.canSeeUuid(ctx.getSender()), false));
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
            final HistoryEntry entry, final boolean name, final boolean uuid, final boolean position) {
        final var builder = new StringBuilder("[white]");

        if (name) {
            builder.append(getName(entry.getAuthor()));
            if (uuid && entry.getAuthor().isPlayer()) {
                builder.append(" [gray](")
                        .append(entry.getAuthor().getUuid().orElseThrow())
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
                    builder, entry.getBlock(), entry.getConfiguration().orElseThrow());
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
            final StringBuilder builder, final Block block, final HistoryConfiguration configuration) {
        builder.append("Configured [accent]")
                .append(block.name)
                .append("[white] to [accent]")
                .append(configuration);
    }

    private String getName(final HistoryAuthor author) {
        return author.isPlayer()
                ? Vars.netServer.admins.getInfo(author.getUuid().orElseThrow()).lastName
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
        // then we reverse the list so latest entries are at the end
        return entries.stream()
                .sorted(Comparator.comparing(HistoryEntry::getTimestamp).reversed())
                .limit(limit)
                .sorted(Comparator.comparing(HistoryEntry::getTimestamp))
                .toList();
    }
}
