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
import com.mongodb.client.model.ReplaceOptions;
import fr.xpdustry.nucleus.api.database.Entity;
import fr.xpdustry.nucleus.api.database.EntityManager;
import java.util.ArrayList;
import java.util.Optional;
import org.bson.BsonDocument;

public class MongoEntityManager<E extends Entity<I>, I> implements EntityManager<E, I> {

    protected static final String ID_FIELD = "_id";

    private final MongoCollection<BsonDocument> collection;
    private final MongoEntityCodec<E> codec;

    protected MongoEntityManager(final MongoCollection<BsonDocument> collection, final MongoEntityCodec<E> codec) {
        this.collection = collection;
        this.codec = codec;
    }

    @Override
    public void save(final E entity) {
        this.collection.replaceOne(
                Filters.eq(ID_FIELD, entity.getIdentifier()), codec.encode(entity), new ReplaceOptions().upsert(true));
    }

    @Override
    public void saveAll(final Iterable<E> entities) {
        entities.forEach(this::save);
    }

    @Override
    public Optional<E> findById(final I id) {
        final var result = this.collection.find(Filters.eq(ID_FIELD, id)).first();
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
        this.collection.deleteOne(Filters.eq(ID_FIELD, id));
    }

    @Override
    public void deleteAll() {
        collection.deleteMany(Filters.empty());
    }

    @Override
    public void deleteAll(final Iterable<E> entities) {
        final var ids = new ArrayList<I>();
        for (final var entity : entities) {
            ids.add(entity.getIdentifier());
        }
        collection.deleteMany(Filters.in(ID_FIELD, ids));
    }

    protected MongoCollection<BsonDocument> getCollection() {
        return this.collection;
    }

    protected MongoEntityCodec<E> getCodec() {
        return this.codec;
    }
}