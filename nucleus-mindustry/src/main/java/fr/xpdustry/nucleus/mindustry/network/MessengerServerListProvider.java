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

import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.scheduler.MindustryTimeUnit;
import fr.xpdustry.distributor.api.scheduler.TaskHandler;
import fr.xpdustry.nucleus.core.messages.ImmutableServerListRequest;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MessengerServerListProvider implements PluginListener, ServerListProvider {

    private final List<String> servers = new CopyOnWriteArrayList<>();
    private final NucleusPlugin nucleus;

    public MessengerServerListProvider(final NucleusPlugin nucleus) {
        this.nucleus = nucleus;
    }

    @TaskHandler(interval = 10L, unit = MindustryTimeUnit.MINUTES, async = true)
    public void onServerListUpdate() {
        this.servers.clear();
        this.nucleus
                .getMessenger()
                .request(ImmutableServerListRequest.of())
                .exceptionally(throwable -> {
                    this.nucleus.getLogger().error("Unable to update the server list", throwable);
                    return List.of("survival", "sandbox", "attack", "pvp");
                })
                .thenAccept(this.servers::addAll);
    }

    @Override
    public List<String> getAvailableServers() {
        return Collections.unmodifiableList(this.servers);
    }
}
