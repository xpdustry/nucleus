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

import com.mongodb.client.MongoCollection;
import fr.xpdustry.nucleus.common.model.User;
import fr.xpdustry.nucleus.common.model.UserManager;
import fr.xpdustry.nucleus.mindustry.mongo.DocumentMongoManager;
import fr.xpdustry.nucleus.mindustry.mongo.MongoEntityCodec;
import java.util.ArrayList;
import org.bson.Document;

public final class DocumentUserManager extends DocumentMongoManager<User, String> implements UserManager {

    public DocumentUserManager(final MongoCollection<Document> collection) {
        super(collection, new Codec());
    }

    @Override
    public User findByIdOrCreate(final String id) {
        return findById(id).orElseGet(() -> new DocumentUser(id));
    }

    static class Codec implements MongoEntityCodec<User> {

        @Override
        public Document encode(final User entity) {
            return new Document()
                    .append("_id", entity.getMongoId())
                    .append("used_names", new ArrayList<>(entity.getUsedNames()))
                    .append(
                            "discord_id",
                            entity.getDiscordId().isPresent()
                                    ? entity.getDiscordId().getAsLong()
                                    : null)
                    .append("used_ips", entity.getUsedIps());
        }

        @Override
        public User decode(final Document entity) {
            final var user = new DocumentUser(entity.getString("_id"));
            user.setUsedNames(entity.getList("used_names", String.class));
            user.setDiscordId(entity.getLong("discord_id"));
            user.setUsedIps(entity.getList("used_ips", String.class));
            return user;
        }
    }
}
