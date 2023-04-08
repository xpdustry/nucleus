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
package fr.xpdustry.nucleus.common.database.mongo;

import com.mongodb.client.MongoCollection;
import fr.xpdustry.nucleus.api.database.model.User;
import fr.xpdustry.nucleus.api.database.model.UserManager;
import org.bson.BsonDocument;

public final class MongoUserManager extends MongoEntityManager<User, String> implements UserManager {

    public MongoUserManager(final MongoCollection<BsonDocument> collection) {
        super(collection, new MongoUserCodec());
    }

    @Override
    public User findByIdOrCreate(final String id) {
        throw new UnsupportedOperationException();
        // return findById(id).orElseGet(() -> User.b.setIdentifier(id));
    }

    private static final class MongoUserCodec implements MongoEntityCodec<User> {

        @Override
        public BsonDocument encode(final User entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User decode(final BsonDocument entity) {
            throw new UnsupportedOperationException();
        }
    }
}
