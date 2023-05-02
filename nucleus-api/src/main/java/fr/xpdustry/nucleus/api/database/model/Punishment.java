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
package fr.xpdustry.nucleus.api.database.model;

import fr.xpdustry.nucleus.api.database.Entity;
import fr.xpdustry.nucleus.api.database.ObjectIdentifier;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Punishment extends Entity<ObjectIdentifier> {

    private final Set<InetAddress> targets = new HashSet<>();
    private Kind kind = Kind.KICK;
    private String reason = "Unknown";
    private Duration duration = Duration.ZERO;
    private boolean pardoned = false;

    public Punishment(final ObjectIdentifier identifier) {
        super(identifier);
    }

    public Set<InetAddress> getTargets() {
        return Collections.unmodifiableSet(this.targets);
    }

    public Punishment setTargets(final Iterable<? extends InetAddress> targets) {
        this.targets.clear();
        targets.forEach(this.targets::add);
        return this;
    }

    public Kind getType() {
        return this.kind;
    }

    public Punishment setType(final Kind kind) {
        this.kind = kind;
        return this;
    }

    public String getReason() {
        return this.reason;
    }

    public Punishment setReason(final String reason) {
        this.reason = reason;
        return this;
    }

    public Duration getDuration() {
        return this.duration;
    }

    public Punishment setDuration(final Duration duration) {
        this.duration = duration;
        return this;
    }

    public boolean isPardoned() {
        return this.pardoned;
    }

    public Punishment setPardoned(final boolean pardoned) {
        this.pardoned = pardoned;
        return this;
    }

    public boolean isExpired() {
        return isPardoned() || getTimestamp().plus(getDuration()).isBefore(Instant.now());
    }

    public Instant getTimestamp() {
        return getIdentifier().getTimestamp();
    }

    public enum Kind {
        MUTE,
        KICK,
        BAN
    }
}
