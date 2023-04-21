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
package fr.xpdustry.nucleus.api.database;

import java.util.Optional;
import java.util.function.UnaryOperator;

public interface EntityManager<I, E extends Entity<I>> {

    void save(final E entity);

    void saveAll(final Iterable<E> entities);

    Optional<E> findById(final I id);

    Iterable<E> findAll();

    boolean exists(final E entity);

    long count();

    void deleteById(final I id);

    void deleteAll();

    void deleteAll(final Iterable<E> entities);

    default void updateIfPresent(final I id, final UnaryOperator<E> updater) {
        findById(id).ifPresent(entity -> this.save(updater.apply(entity)));
    }
}
