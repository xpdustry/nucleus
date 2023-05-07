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
package fr.xpdustry.nucleus.common.database;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface EntityManager<I, E extends Entity<I>> {

    CompletableFuture<Void> save(final E entity);

    CompletableFuture<Void> saveAll(final Iterable<E> entities);

    CompletableFuture<Optional<E>> findById(final I id);

    CompletableFuture<Iterable<E>> findAll();

    CompletableFuture<Boolean> exists(final E entity);

    CompletableFuture<Long> count();

    CompletableFuture<Void> deleteById(final I id);

    CompletableFuture<Void> deleteAll();

    CompletableFuture<Void> deleteAll(final Iterable<E> entities);

    default CompletableFuture<Void> updateIfPresent(final I id, final UnaryOperator<E> updater) {
        return findById(id).thenCompose(result -> result.map(entity -> this.save(updater.apply(entity)))
                .orElse(CompletableFuture.completedFuture(null)));
    }
}
