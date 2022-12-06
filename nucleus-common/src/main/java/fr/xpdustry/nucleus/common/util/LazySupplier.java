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
package fr.xpdustry.nucleus.common.util;

import fr.xpdustry.nucleus.api.database.MongoEntity;
import fr.xpdustry.nucleus.api.database.MongoRepository;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class LazySupplier<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private @MonotonicNonNull T object;

    public LazySupplier(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <E extends MongoEntity<I>, I> LazySupplier<E> retrieve(
            final MongoRepository<E, I> repository, final I id) {
        return new LazySupplier<>(() -> repository.findById(id).orElseThrow());
    }

    @Override
    public T get() {
        return object == null ? object = supplier.get() : object;
    }
}
