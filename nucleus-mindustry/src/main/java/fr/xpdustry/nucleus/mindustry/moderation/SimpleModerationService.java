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
package fr.xpdustry.nucleus.mindustry.moderation;

import arc.Core;
import arc.Events;
import com.google.common.net.InetAddresses;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.util.Priority;
import fr.xpdustry.nucleus.common.application.NucleusListener;
import fr.xpdustry.nucleus.common.database.DatabaseService;
import fr.xpdustry.nucleus.common.database.model.Punishment;
import fr.xpdustry.nucleus.common.database.model.Punishment.Kind;
import fr.xpdustry.nucleus.mindustry.chat.ChatManager;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.inject.Inject;
import mindustry.game.EventType.PlayerBanEvent;
import mindustry.game.EventType.PlayerConnect;
import mindustry.game.EventType.PlayerIpBanEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import net.time4j.PrettyTime;
import org.bson.types.ObjectId;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public final class SimpleModerationService implements ModerationService, NucleusListener {

    private final ChatManager chat;
    private final DatabaseService database;

    @Inject
    public Logger logger;

    @Inject
    public SimpleModerationService(final ChatManager chat, final DatabaseService database) {
        this.chat = chat;
        this.database = database;
    }

    @Override
    public void onNucleusInit() {
        this.chat.addFilter((player, message) -> {
            final var punishment = this.database
                    .getPunishmentManager()
                    .findAllByTarget(InetAddresses.forString(player.ip()))
                    .join()
                    .stream()
                    .filter(Punishment::isActive)
                    .filter(p -> p.getKind() == Kind.MUTE)
                    .max(Comparator.comparing(Punishment::getRemaining));
            punishment.ifPresent(value -> player.sendMessage(
                    "[scarlet]You are muted! Wait " + value.getRemaining().toMinutes() + " minutes to speak again."));
            return punishment.isEmpty();
        });
    }

    @EventHandler(priority = Priority.HIGHEST)
    public void onPlayerConnect(final PlayerConnect event) {
        if (event.player.con().kicked) {
            return;
        }
        this.database
                .getPunishmentManager()
                .findAllByTarget(InetAddresses.forString(event.player.ip()))
                .thenApply(punishments -> punishments.stream()
                        .filter(Punishment::isActive)
                        .filter(p -> p.getKind() == Kind.BAN || p.getKind() == Kind.KICK)
                        .max(Comparator.comparing(Punishment::getKind).thenComparing(Punishment::getRemaining)))
                .thenAcceptAsync(
                        punishment -> punishment.ifPresent(value -> showPunishmentAndKick(event.player, value)),
                        Core.app::post);
    }

    @Override
    public CompletableFuture<Punishment> punish(
            final @Nullable Player sender, final Player target, final Kind kind, final String reason) {
        return this.punish(sender, target, kind, reason, null);
    }

    @Override
    public CompletableFuture<Punishment> punish(
            final @Nullable Player sender,
            final Player target,
            final Kind kind,
            final String reason,
            final @Nullable Duration duration) {
        // TODO
        //  - Implement punishment upgrade when smaller punishment is already active
        //  - Implement punishment lifetime (eg: a mute lasts 1 hour but is considered active for 3 days to be used
        //    as punishment upgrade)
        //  - Broadcast punishment across servers
        return this.createPunishment(target, kind, reason, duration).thenCompose(punishment -> this.database
                .getPunishmentManager()
                .save(punishment)
                .thenAcceptAsync(empty -> this.kickOnlinePlayer(sender, punishment), Core.app::post)
                .thenApply(empty -> punishment));
    }

    private void kickOnlinePlayer(final @Nullable Player sender, final Punishment punishment) {
        if (!(punishment.getKind() == Kind.BAN || punishment.getKind() == Kind.KICK)) {
            return;
        }
        final var addresses =
                punishment.getTargets().stream().map(InetAddress::getHostName).collect(Collectors.toUnmodifiableSet());
        for (final var address : addresses) {
            Events.fire(new PlayerIpBanEvent(address));
        }
        for (final var player : Groups.player) {
            if (addresses.contains(player.ip())) {
                Events.fire(new PlayerBanEvent(player, player.uuid()));
                showPunishmentAndKick(player, punishment);
                if (sender != null) {
                    logger.info(
                            "{} ({}) has {} {} ({}) for '{}'.",
                            sender.plainName(),
                            sender.uuid(),
                            verb(punishment.getKind()),
                            player.plainName(),
                            player.uuid(),
                            punishment.getReason());
                }
                Call.sendMessage("[scarlet]Player " + player.plainName() + " has been " + verb(punishment.getKind())
                        + (sender != null ? " by " + sender.plainName() : ""));
            }
        }
    }

    private String verb(final Kind kind) {
        return switch (kind) {
            case BAN -> "banned";
            case KICK -> "kicked";
            case MUTE -> "muted";
        };
    }

    private CompletableFuture<Punishment> createPunishment(
            final Player target, final Kind kind, final String reason, final @Nullable Duration duration) {
        return this.database
                .getUserManager()
                .findByIdOrCreate(target.uuid())
                .thenCombine(
                        duration == null
                                ? calculateDuration(target, kind)
                                : CompletableFuture.completedFuture(duration),
                        (user, dur) -> new Punishment(new ObjectId())
                                .setDuration(dur)
                                .setReason(reason)
                                .setKind(kind)
                                .setTargets(user.getAddresses()));
    }

    private CompletableFuture<Duration> calculateDuration(final Player target, final Kind kind) {
        if (kind == Kind.MUTE) {
            return CompletableFuture.completedFuture(Duration.ofMinutes(10L));
        } else if (kind == Kind.KICK) {
            return CompletableFuture.completedFuture(Duration.ofHours(1L));
        }
        return this.database
                .getPunishmentManager()
                .findAllByTarget(InetAddresses.forString(target.ip()))
                .thenApply(punishments -> punishments.stream()
                        .map(Punishment::getKind)
                        .filter(Kind.BAN::equals)
                        .count())
                .thenApply(count -> Duration.ofDays(((long) Math.pow(2, count.intValue() + 1)) * 7L));
    }

    private void showPunishmentAndKick(final Player player, final Punishment punishment) {
        player.kick(
                """
                [scarlet]You have been %s for '%s' for %s.

                [white]You can appeal your punishment at [cyan]https://discord.xpdustry.fr[] in the #appeal channel.

                [accent]Your punishment id is [white]%s[]."""
                        .formatted(
                                verb(punishment.getKind()),
                                punishment.getReason(),
                                getPrettyTime().print(punishment.getDuration()),
                                punishment.getIdentifier().toHexString()),
                0);
    }

    // TODO This is horrible, create a service for that please
    private PrettyTime getPrettyTime() {
        final var classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(PrettyTime.class.getClassLoader());
        try {
            return PrettyTime.of(Locale.ENGLISH);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }
}
