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
import java.util.Locale;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

@Immutable(copy = false, builder = false)
@ImmutableNucleusStyle
public abstract sealed class MindustryVersion permits ImmutableMindustryVersion {

    public static MindustryVersion of(final int major, final int build, final int patch, final Type type) {
        return ImmutableMindustryVersion.of(major, build, patch, type);
    }

    public abstract int getMajor();

    public abstract int getBuild();

    public abstract int getPatch();

    public abstract Type getType();

    public enum Type {
        OFFICIAL,
        ALPHA,
        BLEEDING_EDGE,
        CUSTOM
    }

    @Override
    public String toString() {
        return getType().name().toLowerCase(Locale.ROOT).replace('_', '-')
                + " v"
                + getMajor()
                + " "
                + getBuild() + (getPatch() == 0 ? "" : "." + getPatch());
    }

    @Value.Check
    protected void check() {
        if (getMajor() < 0) throw new IllegalArgumentException("Major version must be positive");
        if (getBuild() < 0) throw new IllegalArgumentException("Build version must be positive");
        if (getPatch() < 0) throw new IllegalArgumentException("Patch version must be positive");
    }
}
