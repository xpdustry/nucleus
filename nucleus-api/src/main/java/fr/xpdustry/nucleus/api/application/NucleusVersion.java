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
package fr.xpdustry.nucleus.api.application;

public record NucleusVersion(int year, int month, int build) {
    public static NucleusVersion parse(final String version) {
        final var split = (version.startsWith("v") ? version.substring(1) : version).split("\\.", 3);
        return new NucleusVersion(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    public boolean isNewerThan(final NucleusVersion other) {
        return year > other.year || month > other.month || build > other.build;
    }

    @Override
    public String toString() {
        return "v" + year + "." + month + "." + build;
    }

    public NucleusVersion {
        if (year < 0) {
            throw new IllegalArgumentException("Year must be positive");
        }
        if (month < 0 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (build < 0) {
            throw new IllegalArgumentException("Build must be positive");
        }
    }
}
