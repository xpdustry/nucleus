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
import fr.xpdustry.nucleus.api.database.MongoId;
import fr.xpdustry.nucleus.api.model.Punishment;
import fr.xpdustry.nucleus.api.model.PunishmentRepository;
import fr.xpdustry.nucleus.api.model.User;
import fr.xpdustry.nucleus.api.model.UserRepository;
import fr.xpdustry.nucleus.common.database.BsonMongoId;
import fr.xpdustry.nucleus.common.database.BsonMongoRepository;
import fr.xpdustry.nucleus.common.database.MongoEntityCodec;
import fr.xpdustry.nucleus.common.util.LazySupplier;
import java.time.Duration;
import org.bson.Document;

public final class BsonPunishmentRepository extends BsonMongoRepository<Punishment, MongoId>
        implements PunishmentRepository {

    public BsonPunishmentRepository(final MongoCollection<Document> collection, final UserRepository userRepository) {
        super(collection, new Codec(userRepository));
    }

    public static final class Codec implements MongoEntityCodec<Punishment> {

        private final UserRepository userManager;

        Codec(final UserRepository userManager) {
            this.userManager = userManager;
        }

        @Override
        public Document encode(final Punishment entity) {
            return new Document()
                    .append(
                            "author",
                            entity.getAuthor().map(User::getIdentifier).orElse(null))
                    .append("target", entity.getTarget().getIdentifier())
                    .append("type", entity.getType())
                    .append("reason", entity.getReason())
                    .append("timestamp", entity.getTimestamp())
                    .append("duration", entity.getDuration());
        }

        @Override
        public Punishment decode(final Document entity) {
            return new BsonPunishment(
                    new BsonMongoId(entity.getObjectId("_id")),
                    entity.containsKey("author")
                            ? LazySupplier.retrieve(userManager, new BsonMongoId(entity.getObjectId("author")))
                            : null,
                    LazySupplier.retrieve(userManager, new BsonMongoId(entity.getObjectId("target"))),
                    Punishment.Type.valueOf(entity.getString("type")),
                    entity.getString("reason"),
                    entity.getDate("timestamp").toInstant(),
                    Duration.ofMillis(entity.getLong("duration")));
        }
    }
}
