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
package fr.xpdustry.nucleus.core.model;

import fr.xpdustry.nucleus.core.data.MongoEntity;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;
import org.bson.Document;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class User extends MongoEntity<User, String> {

    private final Set<String> usedNames = new HashSet<>();
    private final Set<String> usedIps = new HashSet<>();
    private @Nullable Long discordId = null;

    public Collection<String> getUsedNames() {
        return Collections.unmodifiableCollection(usedNames);
    }

    public User setUsedNames(Collection<String> usedNames) {
        this.usedNames.clear();
        this.usedNames.addAll(usedNames);
        return this;
    }

    public User addUsedName(String name) {
        usedNames.add(name);
        return this;
    }

    public Collection<String> getUsedIps() {
        return Collections.unmodifiableCollection(usedIps);
    }

    public User setUsedIps(Collection<String> usedIps) {
        this.usedIps.clear();
        this.usedIps.addAll(usedIps);
        return this;
    }

    public User addUsedIp(String ip) {
        usedIps.add(ip);
        return this;
    }

    public OptionalLong getDiscordId() {
        return discordId == null ? OptionalLong.empty() : OptionalLong.of(discordId);
    }

    public User setDiscordId(final @Nullable Long discordId) {
        this.discordId = discordId;
        return this;
    }

    public static final class Codec extends MongoEntity.Codec<User> {

        @Override
        public Document encode(final User entity) {
            return new Document()
                    .append("_id", entity.getIdentifier())
                    .append("usedNames", entity.usedNames)
                    .append("usedIps", entity.usedIps)
                    .append("discordId", entity.discordId);
        }

        @Override
        public User decode(final Document entity) {
            return new User()
                    .setIdentifier(entity.getString("_id"))
                    .setUsedNames(entity.getList("usedNames", String.class))
                    .setUsedIps(entity.getList("usedIps", String.class))
                    .setDiscordId(entity.getLong("discordId"));
        }
    }
}
