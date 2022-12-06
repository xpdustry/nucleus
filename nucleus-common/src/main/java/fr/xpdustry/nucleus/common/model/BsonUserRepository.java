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
package fr.xpdustry.nucleus.common.model;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import fr.xpdustry.nucleus.api.database.MongoId;
import fr.xpdustry.nucleus.api.model.User;
import fr.xpdustry.nucleus.api.model.UserRepository;
import fr.xpdustry.nucleus.common.database.BsonMongoId;
import fr.xpdustry.nucleus.common.database.BsonMongoRepository;
import fr.xpdustry.nucleus.common.database.MongoEntityCodec;
import java.util.ArrayList;
import org.bson.Document;

public final class BsonUserRepository extends BsonMongoRepository<User, MongoId> implements UserRepository {

    public BsonUserRepository(final MongoCollection<Document> collection) {
        super(collection, new UserCodec());
    }

    @Override
    public User findByUuidOrCreate(final String uuid) {
        final var result = getCollection().find(Filters.eq("uuid", uuid)).first();
        return result != null ? getCodec().decode(result) : new BsonUser(new BsonMongoId(), uuid);
    }

    public static final class UserCodec implements MongoEntityCodec<User> {

        @Override
        public Document encode(final User entity) {
            return new Document()
                    .append("_id", entity.getIdentifier())
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
            final var user = new BsonUser(new BsonMongoId(entity.getObjectId("_id")), entity.getString("uuid"));
            user.setUsedNames(entity.getList("used_names", String.class));
            user.setUsedIps(entity.getList("used_ips", String.class));
            user.setDiscordId(entity.getLong("discord_id"));
            return user;
        }
    }
}
