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
package fr.xpdustry.nucleus.discord.interaction.command;

import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.database.DatabaseService;
import fr.xpdustry.nucleus.common.database.model.Punishment;
import fr.xpdustry.nucleus.discord.interaction.InteractionContext;
import fr.xpdustry.nucleus.discord.interaction.InteractionDescription;
import fr.xpdustry.nucleus.discord.interaction.InteractionPermission;
import fr.xpdustry.nucleus.discord.interaction.Option;
import fr.xpdustry.nucleus.discord.interaction.SlashInteraction;
import java.awt.Color;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.inject.Inject;
import org.bson.types.ObjectId;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlashInteraction("punishment")
@InteractionDescription("Manage the punishments of a player.")
@InteractionPermission(PermissionType.MODERATE_MEMBERS)
public final class PunishmentCommand implements NucleusListener {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Logger logger = LoggerFactory.getLogger(PunishmentCommand.class);

    private final DatabaseService databaseService;

    @Inject
    public PunishmentCommand(final DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @SlashInteraction.Handler(subcommand = "list")
    public void onPunishmentList(final InteractionContext context, final @Option("address") String address) {
        final InetAddress target;
        try {
            target = InetAddresses.forString(address);
        } catch (final IllegalArgumentException exception) {
            context.sendEphemeralMessage("The address %s is not valid.", address);
            return;
        }

        context.sendEphemeralMessage("Fetching punishments...")
                .thenCompose(updater -> this.databaseService
                        .getPunishmentManager()
                        .findAllByTarget(target)
                        .thenAccept(punishments -> {
                            final var embeds =
                                    punishments.stream().map(this::toEmbed).toList();
                            updater.addEmbeds(embeds).setContent(embeds.isEmpty() ? "No punishments found." : "");
                        })
                        .thenRun(updater::update))
                // TODO Create a simple CompletableFuture exception handler in the InteractionContext
                .exceptionally(throwable -> {
                    context.sendEphemeralMessage("An error occurred while fetching punishments.");
                    logger.error("An error occurred while fetching punishments.", throwable);
                    return null;
                });
    }

    @SlashInteraction.Handler(subcommand = "pardon")
    public void onPunishmentPardon(final InteractionContext context, final @Option("identifier") String identifier) {
        if (!ObjectId.isValid(identifier)) {
            context.sendEphemeralMessage("The identifier %s is not valid.", identifier);
            return;
        }

        // TODO Properly handle this async call
        final var punishment = databaseService
                .getPunishmentManager()
                .findById(new ObjectId(identifier))
                .join();
        if (punishment.isEmpty()) {
            context.sendEphemeralMessage("The punishment %s does not exist.", identifier);
            return;
        } else if (punishment.get().isPardoned()) {
            context.sendEphemeralMessage("The punishment %s is already pardoned.", identifier);
            return;
        }

        databaseService.getPunishmentManager().save(punishment.get().setPardoned(true));
        context.sendEphemeralMessage("The punishment %s has been pardoned.", identifier);
    }

    private Color getColorFromKind(final Punishment.Kind kind) {
        return switch (kind) {
            case BAN -> Color.RED;
            case KICK -> Color.ORANGE;
            case MUTE -> Color.YELLOW;
        };
    }

    private String formatDuration(final Duration duration) {
        final var builder = new StringBuilder();

        final var days = duration.toDays();
        if (days > 0) {
            builder.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
        }

        final var hours = duration.toHours() % 24;
        if (hours > 0) {
            builder.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
        }

        final var minutes = duration.toMinutes() % 60;
        if (minutes > 0) {
            builder.append(minutes)
                    .append(" minute")
                    .append(minutes > 1 ? "s" : "")
                    .append(" ");
        }

        final var seconds = duration.getSeconds() % 60;
        if (seconds > 0) {
            builder.append(seconds)
                    .append(" second")
                    .append(seconds > 1 ? "s" : "")
                    .append(" ");
        }

        return Strings.emptyToNull(builder.toString());
    }

    private EmbedBuilder toEmbed(final Punishment punishment) {
        return new EmbedBuilder()
                .setTitle(punishment.getKind().name())
                .setColor(getColorFromKind(punishment.getKind()))
                .addField("Identifier", "`" + punishment.getIdentifier().toHexString() + "`")
                .addField("Kind", punishment.getKind().name().toLowerCase(Locale.ROOT))
                .addField("Reason", punishment.getReason())
                .addField(
                        "Date",
                        DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(
                                punishment.getTimestamp(), Clock.systemUTC().getZone())))
                .addField("Duration", formatDuration(punishment.getDuration()))
                .addField("Expired", punishment.isExpired() ? "Yes" : "No")
                .addField("Pardoned", punishment.isPardoned() ? "Yes" : "No");
    }
}
