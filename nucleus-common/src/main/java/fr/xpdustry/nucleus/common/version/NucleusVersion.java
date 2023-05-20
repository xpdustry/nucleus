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
package fr.xpdustry.nucleus.common.version;

import fr.xpdustry.nucleus.common.annotation.ImmutableNucleusStyle;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

@Immutable(copy = false, builder = false)
@ImmutableNucleusStyle
public abstract sealed class NucleusVersion permits ImmutableNucleusVersion {

    public static NucleusVersion of(final int year, final int month, final int build) {
        return ImmutableNucleusVersion.of(year, month, build);
    }

    public static NucleusVersion parse(final String version) {
        final var split = (version.startsWith("v") ? version.substring(1) : version).split("\\.", 3);
        return ImmutableNucleusVersion.of(
                Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    public abstract int getYear();

    public abstract int getMonth();

    public abstract int getBuild();

    public boolean isNewerThan(final NucleusVersion other) {
        return getYear() > other.getYear() || getMonth() > other.getMonth() || getBuild() > other.getBuild();
    }

    @Override
    public String toString() {
        return "v" + getYear() + "." + getMonth() + "." + getBuild();
    }

    @Value.Check
    protected void check() {
        if (getYear() < 0) throw new IllegalArgumentException("Year must be positive");
        if (getMonth() < 0 || getMonth() > 12) throw new IllegalArgumentException("Month must be between 1 and 12");
        if (getBuild() < 0) throw new IllegalArgumentException("Build must be positive");
    }
}
