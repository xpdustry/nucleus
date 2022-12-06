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
package fr.xpdustry.nucleus.mindustry.security;

import fr.xpdustry.distributor.api.plugin.PluginListener;
import fr.xpdustry.distributor.api.util.MoreEvents;
import fr.xpdustry.nucleus.mindustry.NucleusPlugin;
import fr.xpdustry.nucleus.mindustry.mongo.PluginMongoStorage;
import mindustry.game.EventType;

public final class PlayerGatekeeper implements PluginListener {

    private final PluginMongoStorage mongoStorage;

    public PlayerGatekeeper(final NucleusPlugin nucleus) {
        this.mongoStorage = nucleus.getMongoProvider();
    }

    @Override
    public void onPluginInit() {
        // TODO Improve user handling
        MoreEvents.subscribe(EventType.PlayerConnect.class, event -> {
            final var user = mongoStorage.getUserManager().findByUuidOrCreate(event.player.uuid());
            user.addUsedIp(event.player.ip());
            user.addUsedName(event.player.plainName());
            mongoStorage.getUserManager().save(user);
        });
    }
}
