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

public interface EntityRef<E extends Entity<I>, I> {

    static <E extends Entity<I>, I> EntityRef<E, I> empty(final I identifier) {
        return new EntityRef<>() {
            @Override
            public I getIdentifier() {
                return identifier;
            }

            @Override
            public Optional<E> getEntity() {
                return Optional.empty();
            }
        };
    }

    static <E extends Entity<I>, I> EntityRef<E, I> lazy(final I identifier, final EntityManager<I, E> manager) {
        return new EntityRef<>() {

            @Override
            public I getIdentifier() {
                return identifier;
            }

            @Override
            public Optional<E> getEntity() {
                return manager.findById(identifier);
            }
        };
    }

    I getIdentifier();

    Optional<E> getEntity();
}
