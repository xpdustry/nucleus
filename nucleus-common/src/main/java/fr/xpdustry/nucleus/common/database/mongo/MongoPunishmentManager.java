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

import com.google.common.net.InetAddresses;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import fr.xpdustry.nucleus.common.database.model.Punishment;
import fr.xpdustry.nucleus.common.database.model.Punishment.Kind;
import fr.xpdustry.nucleus.common.database.model.PunishmentManager;
import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.types.ObjectId;

public final class MongoPunishmentManager extends MongoEntityManager<Punishment, ObjectId>
        implements PunishmentManager {

    public MongoPunishmentManager(final MongoCollection<BsonDocument> collection, final Executor executor) {
        super(collection, executor, new MongoPunishmentCodec());
    }

    @Override
    public CompletableFuture<List<Punishment>> findAllByTarget(final InetAddress target) {
        return supplyAsync(() -> Collections.unmodifiableList(this.collection
                .find(Filters.in("targets", target.getHostAddress()))
                .map(this.codec::decode)
                .into(new ArrayList<>())));
    }

    public static final class MongoPunishmentCodec implements MongoEntityCodec<Punishment> {

        @Override
        public BsonDocument encode(final Punishment entity) {
            return new BsonDocument()
                    .append(
                            ID_FIELD,
                            new BsonObjectId(new ObjectId(entity.getIdentifier().toHexString())))
                    .append(
                            "targets",
                            new BsonArray(entity.getTargets().stream()
                                    .map(InetAddress::getHostAddress)
                                    .map(BsonString::new)
                                    .toList()))
                    .append("kind", new BsonString(entity.getKind().name()))
                    .append("reason", new BsonString(entity.getReason()))
                    .append("duration", new BsonInt64(entity.getDuration().getSeconds()))
                    .append("pardoned", new BsonBoolean(entity.isPardoned()));
        }

        @Override
        public Punishment decode(final BsonDocument entity) {
            return new Punishment(entity.getObjectId(ID_FIELD).getValue())
                    .setTargets(entity.getArray("targets").stream()
                            .map(BsonValue::asString)
                            .map(BsonString::getValue)
                            .map(InetAddresses::forString)
                            .toList())
                    .setKind(Kind.valueOf(entity.getString("kind").getValue()))
                    .setReason(entity.getString("reason").getValue())
                    .setDuration(Duration.ofSeconds(entity.getInt64("duration").getValue()))
                    .setPardoned(entity.getBoolean("pardoned").getValue());
        }
    }
}
