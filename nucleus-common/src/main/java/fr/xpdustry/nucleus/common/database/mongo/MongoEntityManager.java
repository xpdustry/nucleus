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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import fr.xpdustry.nucleus.api.database.Entity;
import fr.xpdustry.nucleus.api.database.EntityManager;
import fr.xpdustry.nucleus.api.database.ObjectIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;

public class MongoEntityManager<E extends Entity<I>, I> implements EntityManager<I, E> {

    protected static final String ID_FIELD = "_id";

    protected final MongoCollection<BsonDocument> collection;
    protected final MongoEntityCodec<E> codec;

    protected MongoEntityManager(final MongoCollection<BsonDocument> collection, final MongoEntityCodec<E> codec) {
        this.collection = collection;
        this.codec = codec;
    }

    @Override
    public void save(final E entity) {
        this.collection.replaceOne(
                Filters.eq(ID_FIELD, adapt(entity.getIdentifier())),
                codec.encode(entity),
                new ReplaceOptions().upsert(true));
    }

    @Override
    public void saveAll(final Iterable<E> entities) {
        final List<WriteModel<BsonDocument>> writes = new ArrayList<>();
        for (final var entity : entities) {
            final var document = codec.encode(entity);
            writes.add(new ReplaceOneModel<>(
                    Filters.eq(ID_FIELD, document.get(ID_FIELD)), document, new ReplaceOptions().upsert(true)));
        }
        this.collection.bulkWrite(writes);
    }

    @Override
    public Optional<E> findById(final I id) {
        final var result = this.collection.find(Filters.eq(ID_FIELD, adapt(id))).first();
        return result == null ? Optional.empty() : Optional.of(codec.decode(result));
    }

    @Override
    public Iterable<E> findAll() {
        return collection.find().map(codec::decode);
    }

    @Override
    public boolean exists(final E entity) {
        return findById(entity.getIdentifier()).isPresent();
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public void deleteById(final I id) {
        this.collection.deleteOne(Filters.eq(ID_FIELD, adapt(id)));
    }

    @Override
    public void deleteAll() {
        collection.deleteMany(Filters.empty());
    }

    @Override
    public void deleteAll(final Iterable<E> entities) {
        final List<Object> ids = new ArrayList<>();
        for (final var entity : entities) {
            ids.add(adapt(entity.getIdentifier()));
        }
        collection.deleteMany(Filters.in(ID_FIELD, ids));
    }

    // TODO I should probably not do that but meh
    protected Object adapt(final Object object) {
        if (object instanceof final ObjectIdentifier identifier) {
            return new ObjectId(identifier.toHexString());
        }
        return object;
    }
}
