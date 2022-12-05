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
package fr.xpdustry.nucleus.common.mongo;

import java.util.Optional;

public interface MongoManager<T extends MongoEntity<I>, I> {

    void save(final T entity);

    default void saveAll(final Iterable<T> entities) {
        entities.forEach(this::save);
    }

    Optional<T> findById(final I id);

    Iterable<T> findAll();

    default boolean exists(final T entity) {
        return findById(entity.getMongoId()).isPresent();
    }

    default boolean existsById(final I id) {
        return this.findById(id).isPresent();
    }

    long count();

    void deleteById(final I id);

    default void delete(final T entity) {
        this.deleteById(entity.getMongoId());
    }

    void deleteAll();

    default void deleteAll(final Iterable<T> entities) {
        entities.forEach(this::delete);
    }
}
