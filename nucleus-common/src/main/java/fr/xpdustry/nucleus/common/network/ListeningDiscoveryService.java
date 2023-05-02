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
package fr.xpdustry.nucleus.common.network;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import fr.xpdustry.nucleus.api.application.NucleusListener;
import fr.xpdustry.nucleus.api.application.NucleusRuntime;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.api.network.DiscoveryMessage;
import fr.xpdustry.nucleus.api.network.DiscoveryMessage.Type;
import fr.xpdustry.nucleus.api.network.DiscoveryService;
import fr.xpdustry.nucleus.api.network.MindustryServerInfo;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;

public class ListeningDiscoveryService implements DiscoveryService, NucleusListener {

    private final MessageService messageService;
    private final NucleusRuntime runtime;
    private final Cache<String, Optional<MindustryServerInfo>> servers;

    @Inject
    protected Logger logger;

    @Inject
    public ListeningDiscoveryService(final MessageService messageService, final NucleusRuntime runtime) {
        this.messageService = messageService;
        this.runtime = runtime;
        this.servers = Caffeine.newBuilder()
                .expireAfterWrite(45L, TimeUnit.SECONDS)
                .executor(runtime.getAsyncExecutor())
                .removalListener(new DiscoveryRemovalListener())
                .build();
    }

    @SuppressWarnings("OptionalAssignedToNull")
    @Override
    public void onNucleusInit() {
        this.messageService.subscribe(DiscoveryMessage.class, message -> {
            if (message.getType() == Type.DISCOVERY) {
                if (this.servers.getIfPresent(message.getServerIdentifier()) != null) {
                    this.logger.debug(
                            "Received discovery message from already discovered server {}",
                            message.getServerIdentifier());
                } else {
                    this.servers.put(message.getServerIdentifier(), message.getServerInfo());
                    this.logger.debug("Discovered server {}", message.getServerIdentifier());
                    this.onServerDiscovered(message);
                }
            } else if (message.getType() == Type.HEARTBEAT) {
                if (this.servers.getIfPresent(message.getServerIdentifier()) == null) {
                    this.logger.debug(
                            "Received heartbeat message from undiscovered server {}", message.getServerIdentifier());
                } else {
                    this.servers.put(message.getServerIdentifier(), message.getServerInfo());
                    this.logger.debug("Received heartbeat message from server {}", message.getServerIdentifier());
                }
            } else if (message.getType() == Type.DISCONNECT) {
                this.servers.invalidate(message.getServerIdentifier());
                this.logger.debug("Undiscovered server {}", message.getServerIdentifier());
            }
        });
    }

    @Override
    public void heartbeat() {}

    @Override
    public Map<String, MindustryServerInfo> getDiscoveredServers() {
        return servers.asMap().entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toUnmodifiableMap(
                        Entry::getKey, entry -> entry.getValue().get()));
    }

    @Override
    public Optional<MindustryServerInfo> getLocalServer() {
        return Optional.empty();
    }

    protected MessageService getMessageService() {
        return this.messageService;
    }

    protected NucleusRuntime getRuntime() {
        return this.runtime;
    }

    // TODO Cleanup ?
    protected void onServerDiscovered(final DiscoveryMessage message) {}

    private final class DiscoveryRemovalListener implements RemovalListener<String, Optional<MindustryServerInfo>> {

        @Override
        public void onRemoval(final String key, final Optional<MindustryServerInfo> value, final RemovalCause cause) {
            if (cause == RemovalCause.EXPIRED) {
                ListeningDiscoveryService.this.logger.debug("Server {} has timeout.", key);
            }
        }
    }
}
