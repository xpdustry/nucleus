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
package fr.xpdustry.nucleus.api.model;

import fr.xpdustry.nucleus.api.database.MongoEntity;
import fr.xpdustry.nucleus.api.database.MongoId;
import java.util.Collection;
import java.util.OptionalLong;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface User extends MongoEntity<MongoId> {

    String getUuid();

    Collection<String> getUsedNames();

    void setUsedNames(final Collection<String> usedNames);

    void addUsedName(final String name);

    Collection<String> getUsedIps();

    void setUsedIps(final Collection<String> usedIps);

    void addUsedIp(final String ip);

    OptionalLong getDiscordId();

    void setDiscordId(final @Nullable Long id);
}
