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
package fr.xpdustry.nucleus.core.model;

import fr.xpdustry.nucleus.core.data.MongoEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Punishment extends MongoEntity<Punishment, ObjectId> {

    private final List<String> targetIps = new ArrayList<>();
    private @Nullable Supplier<User> author = null;
    private Type type = Type.FROZEN;
    private String reason = "Unknown";
    private Instant timestamp = Instant.now();
    private Duration duration = Duration.ofHours(1L);
    private boolean pardoned = false;

    public Optional<User> getAuthor() {
        return Optional.ofNullable(author).map(Supplier::get);
    }

    public Punishment setAuthor(final @Nullable Supplier<User> author) {
        this.author = author;
        return this;
    }

    public List<String> getTargetIps() {
        return targetIps;
    }

    public Punishment setTargetIps(final List<String> targetIps) {
        this.targetIps.clear();
        this.targetIps.addAll(targetIps);
        return this;
    }

    public Punishment addTargetIp(final String targetIp) {
        targetIps.add(targetIp);
        return this;
    }

    public Type getType() {
        return type;
    }

    public Punishment setType(final Type type) {
        this.type = type;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public Punishment setReason(final String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Punishment setTimestamp(final Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Duration getDuration() {
        return duration;
    }

    public Punishment setDuration(final Duration duration) {
        this.duration = duration;
        return this;
    }

    public boolean isPardoned() {
        return pardoned;
    }

    public Punishment setPardoned(boolean pardoned) {
        this.pardoned = pardoned;
        return this;
    }

    public boolean isExpired() {
        return isPardoned() || getTimestamp().plus(getDuration()).isBefore(Instant.now());
    }

    public enum Type {
        FROZEN,
        KICK,
        BAN
    }

    public static final class Codec extends MongoEntity.Codec<Punishment> {

        private final UserRepository userRepository;

        public Codec(final UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Override
        public Document encode(final Punishment entity) {
            return new Document()
                    .append("_id", entity.getIdentifier())
                    .append(
                            "author",
                            entity.getAuthor().map(User::getIdentifier).orElse(null))
                    .append("target_ips", entity.getTargetIps())
                    .append("type", entity.type)
                    .append("reason", entity.reason)
                    .append("timestamp", entity.timestamp)
                    .append("duration", entity.duration)
                    .append("pardoned", entity.pardoned);
        }

        @Override
        public Punishment decode(Document entity) {
            return new Punishment()
                    .setIdentifier(entity.getObjectId("_id"))
                    .setAuthor(
                            entity.getString("author") != null ? userRepository.lazy(entity.getString("author")) : null)
                    .setTargetIps(entity.getList("target_ips", String.class))
                    .setType(Type.valueOf(entity.getString("type")))
                    .setReason(entity.getString("reason"))
                    .setTimestamp(Instant.ofEpochMilli(entity.getLong("timestamp")))
                    .setDuration(Duration.ofMillis(entity.getLong("duration")))
                    .setPardoned(entity.getBoolean("pardoned"));
        }
    }
}
