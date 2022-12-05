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
package fr.xpdustry.nucleus.mindustry.model;

import fr.xpdustry.nucleus.common.model.User;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DocumentUser implements User {

    private final String uuid;
    private final Set<String> usedNames = new HashSet<>();
    private final Set<String> usedIps = new HashSet<>();
    private @Nullable Long discordId = null;

    public DocumentUser(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getMongoId() {
        return uuid;
    }

    @Override
    public Collection<String> getUsedNames() {
        return Collections.unmodifiableCollection(usedNames);
    }

    @Override
    public void setUsedNames(Collection<String> usedNames) {
        this.usedNames.clear();
        this.usedNames.addAll(usedNames);
    }

    @Override
    public void addUsedName(String name) {
        usedNames.add(name);
    }

    @Override
    public OptionalLong getDiscordId() {
        return discordId == null ? OptionalLong.empty() : OptionalLong.of(discordId);
    }

    @Override
    public void setDiscordId(@Nullable Long id) {
        this.discordId = id;
    }

    @Override
    public Collection<String> getUsedIps() {
        return Collections.unmodifiableCollection(usedIps);
    }

    @Override
    public void setUsedIps(Collection<String> usedIps) {
        this.usedIps.clear();
        this.usedIps.addAll(usedIps);
    }

    @Override
    public void addUsedIp(String ip) {
        usedIps.add(ip);
    }
}
