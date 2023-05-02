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
import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class User extends Entity<String> {

    private final Set<String> names = new HashSet<>();
    private final Set<InetAddress> addresses = new HashSet<>();
    private @Nullable String lastName = null;
    private @Nullable InetAddress lastAddress = null;
    private int timesJoined = 0;
    private int timesKicked = 0;
    private int gamesPlayed = 0;
    private Duration playTime = Duration.ZERO;

    public User(final String uuid) {
        super(uuid);
    }

    public Set<String> getNames() {
        return Collections.unmodifiableSet(this.names);
    }

    public User setNames(final Iterable<String> names) {
        this.names.clear();
        names.forEach(this.names::add);
        return this;
    }

    public User addName(final String name) {
        this.names.add(name);
        return this;
    }

    public User addAllNames(final Iterable<String> names) {
        names.forEach(this.names::add);
        return this;
    }

    public Set<InetAddress> getAddresses() {
        return Collections.unmodifiableSet(this.addresses);
    }

    public User setAddresses(final Iterable<? extends InetAddress> addresses) {
        this.addresses.clear();
        addresses.forEach(this.addresses::add);
        return this;
    }

    public User addAddress(final InetAddress address) {
        this.addresses.add(address);
        return this;
    }

    public User addAllAddresses(final Iterable<? extends InetAddress> addresses) {
        addresses.forEach(this.addresses::add);
        return this;
    }

    public Optional<String> getLastName() {
        return Optional.ofNullable(this.lastName);
    }

    public User setLastName(final String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Optional<InetAddress> getLastAddress() {
        return Optional.ofNullable(this.lastAddress);
    }

    public User setLastAddress(final InetAddress lastAddress) {
        this.lastAddress = lastAddress;
        return this;
    }

    public int getTimesJoined() {
        return this.timesJoined;
    }

    public User setTimesJoined(final int timesJoined) {
        this.timesJoined = timesJoined;
        return this;
    }

    public int getTimesKicked() {
        return this.timesKicked;
    }

    public User setTimesKicked(final int timesKicked) {
        this.timesKicked = timesKicked;
        return this;
    }

    public int getGamesPlayed() {
        return this.gamesPlayed;
    }

    public User setGamesPlayed(final int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
        return this;
    }

    public Duration getPlayTime() {
        return this.playTime;
    }

    public User setPlayTime(final Duration playTime) {
        this.playTime = playTime;
        return this;
    }
}
