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
package fr.xpdustry.nucleus.api.database.model;

import fr.xpdustry.nucleus.api.database.Entity;
import java.util.Collection;
import java.util.OptionalLong;

public interface User extends Entity<String> {

    Collection<String> getUsedNames();

    User setUsedNames(final Iterable<String> usedNames);

    User addUsedName(final String name);

    Collection<String> getUsedIps();

    User setUsedIps(final Iterable<String> usedIps);

    User addUsedIp(String ip);

    OptionalLong getDiscordId();

    User setDiscordId(final OptionalLong discordId);

    User setDiscordId(final long discordId);

    interface Builder extends Entity.Builder<String, User, Builder> {

        Builder withUsedNames(final Iterable<String> usedNames);

        Builder withUsedIps(final Iterable<String> usedIps);

        Builder withDiscordId(final OptionalLong discordId);
    }
}
