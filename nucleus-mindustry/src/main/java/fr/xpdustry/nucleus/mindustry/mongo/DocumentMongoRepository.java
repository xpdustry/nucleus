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
package fr.xpdustry.nucleus.mindustry.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import fr.xpdustry.nucleus.common.mongo.MongoEntity;
import fr.xpdustry.nucleus.common.mongo.MongoRepository;
import java.util.ArrayList;
import java.util.Optional;
import org.bson.Document;

public abstract class DocumentMongoRepository<E extends MongoEntity<I>, I> implements MongoRepository<E, I> {

    private final MongoCollection<Document> collection;
    private final MongoEntityCodec<E> codec;

    public DocumentMongoRepository(final MongoCollection<Document> collection, final MongoEntityCodec<E> codec) {
        this.collection = collection;
        this.codec = codec;
    }

    @Override
    public void save(final E entity) {
        this.collection.replaceOne(
                Filters.eq("_id", entity.getMongoId()), codec.encode(entity), new ReplaceOptions().upsert(true));
    }

    @Override
    public Optional<E> findById(final I id) {
        final var result = this.collection.find(Filters.eq("_id", id)).first();
        return result == null ? Optional.empty() : Optional.of(codec.decode(result));
    }

    @Override
    public Iterable<E> findAll() {
        return collection.find().map(codec::decode);
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public void deleteById(final I id) {
        this.collection.deleteOne(Filters.eq("_id", id));
    }

    @Override
    public void deleteAll() {
        collection.deleteMany(Filters.empty());
    }

    @Override
    public void deleteAll(final Iterable<E> entities) {
        final var ids = new ArrayList<I>();
        for (final var entity : entities) {
            ids.add(entity.getMongoId());
        }
        collection.deleteMany(Filters.in("_id", ids));
    }

    protected MongoCollection<Document> getCollection() {
        return this.collection;
    }

    protected MongoEntityCodec<E> getCodec() {
        return this.codec;
    }
}
