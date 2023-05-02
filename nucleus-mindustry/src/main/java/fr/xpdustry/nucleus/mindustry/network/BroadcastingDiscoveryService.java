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
package fr.xpdustry.nucleus.mindustry.network;

import fr.xpdustry.distributor.api.DistributorProvider;
import fr.xpdustry.distributor.api.plugin.MindustryPlugin;
import fr.xpdustry.distributor.api.scheduler.MindustryTimeUnit;
import fr.xpdustry.distributor.api.scheduler.PluginTask;
import fr.xpdustry.nucleus.api.application.MindustryVersion;
import fr.xpdustry.nucleus.api.application.NucleusRuntime;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.api.network.DiscoveryMessage;
import fr.xpdustry.nucleus.api.network.MindustryServerInfo;
import fr.xpdustry.nucleus.api.network.MindustryServerInfo.GameMode;
import fr.xpdustry.nucleus.common.network.ListeningDiscoveryService;
import fr.xpdustry.nucleus.mindustry.NucleusPluginConfiguration;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Groups;
import mindustry.net.Administration.Config;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class BroadcastingDiscoveryService extends ListeningDiscoveryService {

    private final MindustryPlugin plugin;
    private final NucleusPluginConfiguration configuration;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private @MonotonicNonNull String host = null;
    private @MonotonicNonNull PluginTask<Void> task = null;

    @Inject
    public BroadcastingDiscoveryService(
            final MessageService message,
            final NucleusRuntime runtime,
            final MindustryPlugin plugin,
            final NucleusPluginConfiguration configuration) {
        super(message, runtime);
        this.plugin = plugin;
        this.configuration = configuration;
    }

    @Override
    public void onNucleusInit() {
        super.onNucleusInit();
        // https://www.ipify.org/
        try (final var scanner = new Scanner(new URL("https://api.ipify.org").openStream(), StandardCharsets.UTF_8)
                .useDelimiter("\\A")) {
            this.host = scanner.next();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to get server ip.", e);
        }
        // Delay the first heartbeat to avoid spamming the network
        DistributorProvider.get()
                .getPluginScheduler()
                .scheduleSync(plugin)
                .delay(new Random().nextLong(10L), MindustryTimeUnit.SECONDS)
                .execute(() -> this.task = DistributorProvider.get()
                        .getPluginScheduler()
                        .scheduleSync(plugin)
                        .repeat(30L, MindustryTimeUnit.SECONDS)
                        .execute(this::heartbeat));
    }

    @Override
    public void onNucleusExit() {
        super.onNucleusExit();
        if (this.task != null) {
            this.task.cancel(true);
        }
        sendDiscovery(DiscoveryMessage.Type.DISCONNECT);
    }

    @Override
    public void heartbeat() {
        this.sendDiscovery(started.getAndSet(true) ? DiscoveryMessage.Type.HEARTBEAT : DiscoveryMessage.Type.DISCOVERY);
    }

    @Override
    public Optional<MindustryServerInfo> getLocalServer() {
        if (Vars.state.isGame()) {
            final var builder = MindustryServerInfo.builder()
                    .setName(Config.serverName.string())
                    .setHost(this.host)
                    .setPort(Config.port.num())
                    .setMapName(Vars.state.map.name())
                    .setDescription(Config.desc.string())
                    .setWave(Vars.state.wave)
                    .setPlayerCount(Groups.player.size())
                    .setPlayerLimit(Vars.netServer.admins.getPlayerLimit())
                    .setGameVersion(getVersion())
                    .setGameMode(getGameMode());
            if (Vars.state.rules.modeName != null) {
                builder.setGameModeName(Vars.state.rules.modeName);
            }
            return Optional.of(builder.build());
        }
        return Optional.empty();
    }

    @Override
    protected void onServerDiscovered(final DiscoveryMessage message) {
        this.sendDiscovery(DiscoveryMessage.Type.DISCOVERY);
    }

    private void sendDiscovery(final DiscoveryMessage.Type type) {
        this.logger.debug("Sending {} discovery message.", type.name().toLowerCase(Locale.ROOT));
        final var builder = DiscoveryMessage.builder()
                .setServerIdentifier(this.configuration.getServerName())
                .setNucleusVersion(this.getRuntime().getVersion())
                .setType(type);
        getLocalServer().ifPresent(builder::setServerInfo);
        this.getMessageService().publish(builder.build());
    }

    private GameMode getGameMode() {
        return switch (Vars.state.rules.mode()) {
            case attack -> GameMode.ATTACK;
            case pvp -> GameMode.PVP;
            case sandbox -> GameMode.SANDBOX;
            case survival -> GameMode.SURVIVAL;
            case editor -> GameMode.EDITOR;
        };
    }

    private MindustryVersion getVersion() {
        final var builder = MindustryVersion.builder()
                .setMajor(Version.number)
                .setBuild(Math.max(Version.build, 0))
                .setPatch(Version.revision);
        if (Version.build < 0) {
            builder.setType(MindustryVersion.Type.CUSTOM);
        } else {
            builder.setType(
                    // mindustry.core.Version is confusing...
                    switch (Version.modifier.toLowerCase(Locale.ROOT)) {
                        case "alpha" -> MindustryVersion.Type.ALPHA;
                        default -> MindustryVersion.Type.CUSTOM;
                        case "release" -> switch (Version.type) {
                            case "official" -> MindustryVersion.Type.OFFICIAL;
                            case "bleeding-edge" -> MindustryVersion.Type.BLEEDING_EDGE;
                            default -> MindustryVersion.Type.CUSTOM;
                        };
                    });
        }
        return builder.build();
    }
}
