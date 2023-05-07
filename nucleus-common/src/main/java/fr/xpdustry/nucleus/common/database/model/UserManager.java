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
package fr.xpdustry.nucleus.common.database.model;

import fr.xpdustry.nucleus.common.database.EntityManager;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface UserManager extends EntityManager<String, User> {

    default CompletableFuture<User> findByIdOrCreate(final String id) {
        return findById(id).thenApply(result -> result.orElseGet(() -> new User(id)));
    }

    default CompletableFuture<Void> updateOrCreate(final String id, final UnaryOperator<User> updater) {
        return findByIdOrCreate(id).thenApply(updater).thenCompose(this::save);
    }
}
