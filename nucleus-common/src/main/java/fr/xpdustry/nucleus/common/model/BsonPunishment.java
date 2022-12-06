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
package fr.xpdustry.nucleus.common.model;

import fr.xpdustry.nucleus.api.database.MongoId;
import fr.xpdustry.nucleus.api.model.Punishment;
import fr.xpdustry.nucleus.api.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BsonPunishment implements Punishment {

    private final MongoId mongoId;
    private final @Nullable Supplier<User> author;
    private final Supplier<User> target;
    private final Type type;
    private final String reason;
    private final Instant timestamp;
    private final Duration duration;

    public BsonPunishment(
            MongoId mongoId,
            @Nullable Supplier<User> author,
            Supplier<User> target,
            Type type,
            String reason,
            Instant timestamp,
            Duration duration) {
        this.mongoId = mongoId;
        this.author = author;
        this.target = target;
        this.type = type;
        this.reason = reason;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    @Override
    public MongoId getIdentifier() {
        return mongoId;
    }

    @Override
    public Optional<User> getAuthor() {
        return Optional.ofNullable(author).map(Supplier::get);
    }

    @Override
    public User getTarget() {
        return target.get();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }
}
