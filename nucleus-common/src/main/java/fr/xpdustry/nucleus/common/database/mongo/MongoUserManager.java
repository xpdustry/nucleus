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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.BsonValue;

public final class MongoUserManager extends MongoEntityManager<User, String> implements UserManager {

    public MongoUserManager(final MongoCollection<BsonDocument> collection) {
        super(collection, new MongoUserCodec());
    }

    @Override
    public User findByIdOrCreate(final String id) {
        return findById(id).orElseGet(() -> User.builder()
                .setIdentifier(id)
                .setLastName("<unknown>")
                .setNames(Collections.emptyList())
                .setLastAddress(InetAddress.getLoopbackAddress())
                .setAddresses(Collections.emptyList())
                .setTimesJoined(0)
                .setTimesKicked(0)
                .setGamesPlayed(0)
                .setPlayTime(Duration.ZERO)
                .build());
    }

    private static final class MongoUserCodec implements MongoEntityCodec<User> {

        @Override
        public BsonDocument encode(final User entity) {
            return new BsonDocument()
                    .append(ID_FIELD, new BsonString(entity.getIdentifier()))
                    .append("last_name", new BsonString(entity.getLastName()))
                    .append(
                            "names",
                            new BsonArray(entity.getNames().stream()
                                    .map(BsonString::new)
                                    .toList()))
                    .append(
                            "last_address",
                            new BsonString(entity.getLastAddress().getHostAddress()))
                    .append(
                            "addresses",
                            new BsonArray(entity.getAddresses().stream()
                                    .map(address -> new BsonString(address.getHostAddress()))
                                    .toList()))
                    .append("times_joined", new BsonInt32(entity.getTimesJoined()))
                    .append("times_kicked", new BsonInt32(entity.getTimesKicked()))
                    .append("games_played", new BsonInt32(entity.getGamesPlayed()))
                    .append("play_time", new BsonInt64(entity.getPlayTime().toSeconds()));
        }

        @Override
        public User decode(final BsonDocument entity) {
            return User.builder()
                    .setIdentifier(entity.getString(ID_FIELD).getValue())
                    .setLastName(entity.getString("last_name").getValue())
                    .setNames(entity.getArray("names").stream()
                            .map(BsonValue::asString)
                            .map(BsonString::getValue)
                            .toList())
                    .setLastAddress(
                            toInetAddress(entity.getString("last_address").getValue()))
                    .setAddresses(entity.getArray("addresses").stream()
                            .map(BsonValue::asString)
                            .map(BsonString::getValue)
                            .map(this::toInetAddress)
                            .toList())
                    .setTimesJoined(entity.getInt32("times_joined").getValue())
                    .setTimesKicked(entity.getInt32("times_kicked").getValue())
                    .setGamesPlayed(entity.getInt32("games_played").getValue())
                    .setPlayTime(Duration.ofSeconds(entity.getInt64("play_time").getValue()))
                    .build();
        }

        private InetAddress toInetAddress(final String address) {
            try {
                return InetAddress.getByName(address);
            } catch (final UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
