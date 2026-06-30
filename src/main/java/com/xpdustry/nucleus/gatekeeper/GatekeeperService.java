// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper;

import arc.Core;
import arc.func.Cons2;
import arc.struct.ObjectMap;
import arc.util.Strings;
import com.xpdustry.foundation.player.MUUID;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.foundation.util.Priority;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
import com.xpdustry.nucleus.network.InetAddressInfoProvider;
import com.xpdustry.nucleus.network.InetAddressWhitelist;
import com.xpdustry.nucleus.text.BadWordCategory;
import com.xpdustry.nucleus.text.BadWordFinder;
import java.net.InetAddress;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import mindustry.Vars;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import mindustry.net.Packets;

public final class GatekeeperService implements PluginListener {

    private static final Pattern LINK_PATTERN = Pattern.compile("(https?://|discord\\.gg)");
    private static final Set<String> CRACKED_CLIENT_USERNAMES = Set.of(
            "valve", "tuttop", "codex", "igggames", "igg-games.com", "igruhaorg", "freetp.org", "goldberg", "rog");

    private final GatekeeperPipeline pipeline;
    private final ConfigManager configManager;
    private final BadWordFinder badWords;
    private final InetAddressInfoProvider addressInfoProvider;
    private final InetAddressWhitelist addressWhitelist;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public GatekeeperService(
            final GatekeeperPipeline pipeline,
            final ConfigManager configManager,
            final BadWordFinder badWords,
            final InetAddressInfoProvider addressInfoProvider,
            final InetAddressWhitelist addressWhitelist) {
        this.pipeline = pipeline;
        this.configManager = configManager;
        this.badWords = badWords;
        this.addressInfoProvider = addressInfoProvider;
        this.addressWhitelist = addressWhitelist;
    }

    @Override
    public void onInit() {
        final var previous = accessPacketHandlers().get(Packets.ConnectPacket.class);
        Vars.net.handleServer(Packets.ConnectPacket.class, (connection, packet) -> {
            if (connection.kicked) {
                return;
            }
            if (!MUUID.isUuid(packet.uuid) || !MUUID.isUsid(packet.usid)) {
                connection.kick("Invalid UUID or USID", 5_000L);
                return;
            }
            final var address = InetAddress.ofLiteral(connection.address);
            final var muuid = MUUID.of(packet.uuid, packet.usid);
            this.executor.execute(() -> {
                final var context = new GatekeeperContext(Strings.stripColors(packet.name), muuid, address);
                switch (this.pipeline.pump(context)) {
                    case GatekeeperResult.Success _ -> Core.app.post(() -> previous.get(connection, packet));
                    case GatekeeperResult.Failure failure ->
                        connection.kick(failure.reason(), failure.duration().toMillis());
                }
            });
        });

        this.pipeline.register("cracked-client-name", Priority.HIGH, context -> {
            if (!CRACKED_CLIENT_USERNAMES.contains(context.name().toLowerCase(Locale.ROOT))) {
                return GatekeeperResult.SUCCESS;
            }
            return new GatekeeperResult.Failure("""
                [green]Mindustry is a free and open source game.
                [white]It is available on [royal]https://anuke.itch.io/mindustry[].
                [red]Please, get a legit copy of the game.
                """);
        });

        this.pipeline.register("link-in-name", Priority.HIGH, context -> {
            if (LINK_PATTERN.matcher(context.name().toLowerCase(Locale.ROOT)).find()) {
                return new GatekeeperResult.Failure("Your name cannot contain a link.");
            }
            return GatekeeperResult.SUCCESS;
        });

        this.pipeline.register("bad-word-name", Priority.HIGH, context -> {
            final var words = this.badWords.findBadWords(context.name(), EnumSet.allOf(BadWordCategory.class));
            if (words.isEmpty()) {
                return GatekeeperResult.SUCCESS;
            }
            return new GatekeeperResult.Failure(
                    "Your name contains prohibited words, " + words + ". Please change it.");
        });

        this.pipeline.register("safe-ip", Priority.LOW, context -> {
            if (this.addressWhitelist.contains(context.address())) {
                return GatekeeperResult.SUCCESS;
            }
            final var result = this.addressInfoProvider.get(context.address());
            if (result.isPresent()) {
                if (result.get().safe()) {
                    return GatekeeperResult.SUCCESS;
                } else {
                    return new GatekeeperResult.Failure("""
                            [red]VPN detected.[]
                            [lightgray]If you think this is a false positive or using a VPN is necessary to you,
                            join our discord server at [accent]%s[].
                            Then ask for an IP unblock in the [accent]#appeals[] channel.
                            [red]Warning: During the process, only share you IP address to an admin [orange](%s).[].[]
                            """.formatted(
                                    this.configManager.get(ConfigPropertyKey.SERVER_DISCORD),
                                    context.address().getHostAddress()));
                }
            } else {
                return switch (this.configManager.get(ConfigPropertyKey.GATEKEEPER_FAILURE_POLICY)) {
                    case ALLOW_ALL -> GatekeeperResult.SUCCESS;
                    case ALLOW_KNOWN_PLAYERS -> {
                        // TODO Implement using the MindustryUserRepository
                        yield GatekeeperResult.SUCCESS;
                    }
                };
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static ObjectMap<Class<?>, Cons2<NetConnection, Object>> accessPacketHandlers() {
        try {
            final var serverListenersField = Net.class.getDeclaredField("serverListeners");
            serverListenersField.setAccessible(true);
            return (ObjectMap<Class<?>, Cons2<NetConnection, Object>>) serverListenersField.get(Vars.net);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access Net#serverListeners", e);
        }
    }
}
