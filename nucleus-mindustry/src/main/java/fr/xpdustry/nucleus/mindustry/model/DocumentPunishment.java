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
package fr.xpdustry.nucleus.mindustry.model;

import fr.xpdustry.nucleus.common.model.Punishment;
import fr.xpdustry.nucleus.common.model.User;
import fr.xpdustry.nucleus.common.mongo.MongoId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DocumentPunishment implements Punishment {

    private final MongoId mongoId;
    private final @Nullable User author;
    private final User target;
    private final Type type;
    private final Reason reason;
    private final @Nullable String details;
    private final Instant date;
    private final Duration duration;

    public DocumentPunishment(
            MongoId mongoId,
            @Nullable User author,
            User target,
            Type type,
            Reason reason,
            @Nullable String details,
            Instant date,
            Duration duration) {
        this.mongoId = mongoId;
        this.author = author;
        this.target = target;
        this.type = type;
        this.reason = reason;
        this.details = details;
        this.date = date;
        this.duration = duration;
    }

    @Override
    public MongoId getMongoId() {
        return mongoId;
    }

    @Override
    public Optional<User> getAuthor() {
        return Optional.ofNullable(author);
    }

    @Override
    public User getTarget() {
        return target;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Reason getReason() {
        return reason;
    }

    @Override
    public Optional<String> getDetails() {
        return Optional.ofNullable(details);
    }

    @Override
    public Instant getDate() {
        return date;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }
}
