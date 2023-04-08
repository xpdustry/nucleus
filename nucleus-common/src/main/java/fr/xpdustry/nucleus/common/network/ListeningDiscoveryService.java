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
import fr.xpdustry.nucleus.api.application.NucleusRuntime;
import fr.xpdustry.nucleus.api.application.lifecycle.LifecycleListener;
import fr.xpdustry.nucleus.api.message.MessageService;
import fr.xpdustry.nucleus.api.network.DiscoveryMessage;
import fr.xpdustry.nucleus.api.network.DiscoveryMessage.Type;
import fr.xpdustry.nucleus.api.network.DiscoveryService;
import fr.xpdustry.nucleus.api.network.MindustryServer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.slf4j.Logger;

public class ListeningDiscoveryService implements DiscoveryService, LifecycleListener {

    private final MessageService message;
    private final Cache<String, MindustryServer> servers;

    @Inject
    private Logger logger;

    @Inject
    public ListeningDiscoveryService(final MessageService message, final NucleusRuntime runtime) {
        this.message = message;
        this.servers = Caffeine.newBuilder()
                .expireAfterWrite(1L, TimeUnit.MINUTES)
                .executor(runtime.getAsyncExecutor())
                .removalListener(new DiscoveryRemovalListener())
                .build();
    }

    @Override
    public void onLifecycleInit() {
        this.message.subscribe(DiscoveryMessage.class, message -> {
            if (message.getType() == Type.DISCOVERY) {
                this.servers.put(message.getServer().getIdentifier(), message.getServer());
                this.logger.info("Discovered server {}", message.getServer());
            } else {
                if (this.servers.getIfPresent(message.getServer().getIdentifier()) == null) {
                    this.logger.warn("Received heartbeat message from undiscovered server {}", message.getServer());
                } else {
                    this.servers.put(message.getServer().getIdentifier(), message.getServer());
                    this.logger.debug("Received heartbeat message from server {}", message.getServer());
                }
            }
        });
    }

    @Override
    public Collection<MindustryServer> getDiscoveredServers() {
        return List.copyOf(servers.asMap().values());
    }

    private final class DiscoveryRemovalListener implements RemovalListener<String, MindustryServer> {

        @Override
        public void onRemoval(final String key, final MindustryServer value, final RemovalCause cause) {
            if (cause == RemovalCause.EXPIRED) {
                ListeningDiscoveryService.this.logger.info(
                        "Server {} has been removed from the discovery service", value);
            }
        }
    }
}
