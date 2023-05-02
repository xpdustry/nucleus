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
import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.event.EventHandler;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.nucleus.api.application.EnableScanning;
import fr.xpdustry.nucleus.api.application.NucleusListener;
import fr.xpdustry.nucleus.api.database.DatabaseService;
import fr.xpdustry.nucleus.api.database.model.Punishment;
import fr.xpdustry.nucleus.api.database.model.Punishment.Kind;
import fr.xpdustry.nucleus.mindustry.chat.ChatManager;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import mindustry.game.EventType.PlayerBanEvent;
import mindustry.game.EventType.PlayerConnect;
import mindustry.game.EventType.PlayerIpBanEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Packets.KickReason;
import net.time4j.PrettyTime;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

@EnableScanning
public final class SimpleModerationService implements ModerationService, NucleusListener {

    private final ChatManager chat;
    private final DatabaseService database;
    private final MindustryPlugin plugin;

    @Inject
    public Logger logger;

    @Inject
    public SimpleModerationService(
            final ChatManager chat, final DatabaseService database, final MindustryPlugin plugin) {
        this.chat = chat;
        this.database = database;
        this.plugin = plugin;
    }

    @Override
    public void onNucleusInit() {
        this.chat.addFilter((player, message) -> {
            final var punishment =
                    this.database.getPunishmentManager().findAllByTarget(InetAddresses.forString(player.ip())).stream()
                            .filter(Punishment::isActive)
                            .filter(p -> p.getKind() == Kind.MUTE)
                            .max(Comparator.comparing(Punishment::getRemaining));
            punishment.ifPresent(value -> player.sendMessage(
                    "[scarlet]You are muted! Wait " + value.getRemaining().toMinutes() + " minutes to speak again."));
            return punishment.isPresent();
        });
    }

    @EventHandler
    public void onPlayerConnect(final PlayerConnect event) {
        this.database.getPunishmentManager().findAllByTarget(InetAddresses.forString(event.player.ip())).stream()
                .filter(Punishment::isActive)
                .filter(p -> p.getKind() == Kind.BAN || p.getKind() == Kind.KICK)
                .max(Comparator.comparing(Punishment::getKind).thenComparing(Punishment::getRemaining))
                .ifPresent(value -> showPunishmentAndKick(event.player, value));
    }

    @Override
    public CompletableFuture<Punishment> punish(
            final @Nullable Player sender, final Player target, final Kind kind, String reason) {
        // TODO Implement punishment upgrade when smaller punishment is already active
        return this.supplyAsync(() -> {
            final var user = database.getUserManager().findByIdOrCreate(target.uuid());
            final var punishment = new Punishment(
                            database.getObjectIdentifierGenerator().generate())
                    .setDuration(calculateDuration(target, kind))
                    .setReason(reason)
                    .setTargets(user.getAddresses());
            this.database.getPunishmentManager().save(punishment);

            Core.app.post(() -> {
                if (!(kind == Kind.BAN || kind == Kind.KICK)) {
                    return;
                }
                final var addresses = user.getAddresses().stream()
                        .map(InetAddress::getHostName)
                        .collect(Collectors.toUnmodifiableSet());
                for (final var address : addresses) {
                    Events.fire(new PlayerIpBanEvent(address));
                }
                for (final var player : Groups.player) {
                    if (player.uuid().equals(target.uuid()) || addresses.contains(player.ip())) {
                        Events.fire(new PlayerBanEvent(player, player.uuid()));
                        showPunishmentAndKick(player, punishment);
                        if (sender != null) {
                            logger.info(
                                    "{} ({}) has {} {} ({}) for '{}'.",
                                    sender.plainName(),
                                    sender.uuid(),
                                    verb(kind),
                                    target.plainName(),
                                    target.uuid(),
                                    reason);
                        }
                        Call.sendMessage("[scarlet]Player " + target.plainName() + " has been " + verb(kind)
                                + (sender != null ? " by " + sender.plainName() : ""));
                    }
                }
            });

            return punishment;
        });
    }

    private <T> CompletableFuture<T> supplyAsync(final Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, runnable -> DistributorProvider.get()
                .getPluginScheduler()
                .scheduleAsync(this.plugin)
                .execute(runnable));
    }

    private String verb(final Kind kind) {
        return switch (kind) {
            case BAN -> "banned";
            case KICK -> "kicked";
            case MUTE -> "muted";
        };
    }

    @SuppressWarnings("LongDoubleConversion")
    private Duration calculateDuration(final Player target, final Kind kind) {
        if (kind == Kind.MUTE) {
            return Duration.ofMinutes(10L);
        } else if (kind == Kind.KICK) {
            return Duration.ofHours(1L);
        }
        final var bans =
                this.database.getPunishmentManager().findAllByTarget(InetAddresses.forString(target.ip())).stream()
                        .map(Punishment::getKind)
                        .filter(Kind.BAN::equals)
                        .count();
        return Duration.ofDays(((long) Math.pow(2, bans + 1)) * 7L);
    }

    private void showPunishmentAndKick(final Player player, final Punishment punishment) {
        Call.infoMessage(
                player.con,
                """
                [scarlet]You have been %s for '%s' for %s.

                [white]You can appeal your punishment at [cyan]https://discord.xpdustry.fr[] in the #appeal channel.

                [accent]Your punishment id is [white]%s[]."""
                        .formatted(
                                verb(punishment.getKind()),
                                punishment.getReason(),
                                getPrettyTime().print(punishment.getDuration()),
                                punishment.getIdentifier().toHexString()));
        player.kick(KickReason.gameover);
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
