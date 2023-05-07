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
import fr.xpdustry.nucleus.common.database.Entity;
import fr.xpdustry.nucleus.common.database.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.bson.BsonDocument;

public class MongoEntityManager<E extends Entity<I>, I> implements EntityManager<I, E> {

    protected static final String ID_FIELD = "_id";

    protected final MongoCollection<BsonDocument> collection;
    protected final MongoEntityCodec<E> codec;
    private final Executor executor;

    protected MongoEntityManager(
            final MongoCollection<BsonDocument> collection, final Executor executor, final MongoEntityCodec<E> codec) {
        this.collection = collection;
        this.codec = codec;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> save(final E entity) {
        return runAsync(() -> this.collection.replaceOne(
                Filters.eq(ID_FIELD, entity.getIdentifier()), codec.encode(entity), new ReplaceOptions().upsert(true)));
    }

    @Override
    public CompletableFuture<Void> saveAll(final Iterable<E> entities) {
        return runAsync(() -> {
            final List<WriteModel<BsonDocument>> writes = new ArrayList<>();
            for (final var entity : entities) {
                final var document = codec.encode(entity);
                writes.add(new ReplaceOneModel<>(
                        Filters.eq(ID_FIELD, document.get(ID_FIELD)), document, new ReplaceOptions().upsert(true)));
            }
            this.collection.bulkWrite(writes);
        });
    }

    @Override
    public CompletableFuture<Optional<E>> findById(final I id) {
        return supplyAsync(() -> Optional.ofNullable(
                        this.collection.find(Filters.eq(ID_FIELD, id)).first())
                .map(codec::decode));
    }

    @Override
    public CompletableFuture<Iterable<E>> findAll() {
        return supplyAsync(() -> collection.find().map(codec::decode));
    }

    @Override
    public CompletableFuture<Boolean> exists(final E entity) {
        return findById(entity.getIdentifier()).thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Long> count() {
        return supplyAsync(collection::countDocuments);
    }

    @Override
    public CompletableFuture<Void> deleteById(final I id) {
        return runAsync(() -> this.collection.deleteOne(Filters.eq(ID_FIELD, id)));
    }

    @Override
    public CompletableFuture<Void> deleteAll() {
        return runAsync(() -> collection.deleteMany(Filters.empty()));
    }

    @Override
    public CompletableFuture<Void> deleteAll(final Iterable<E> entities) {
        return runAsync(() -> {
            final List<Object> ids = new ArrayList<>();
            for (final var entity : entities) {
                ids.add(entity.getIdentifier());
            }
            collection.deleteMany(Filters.in(ID_FIELD, ids));
        });
    }

    protected <T> CompletableFuture<T> supplyAsync(final Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, this.executor);
    }

    protected CompletableFuture<Void> runAsync(final Runnable runnable) {
        return CompletableFuture.runAsync(runnable, this.executor);
    }
}
