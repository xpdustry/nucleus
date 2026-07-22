// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.version;

import java.util.Comparator;

/// A calendar-based version for nucleus.
public record NucleusVersion(int year, int month, int build) implements Comparable<NucleusVersion> {

    private static final Comparator<NucleusVersion> COMPARATOR = Comparator.comparingInt(NucleusVersion::year)
            .thenComparingInt(NucleusVersion::month)
            .thenComparingInt(NucleusVersion::build);

    public NucleusVersion {
        if (year < 0) {
            throw new IllegalArgumentException("Year must be positive");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (build < 0) {
            throw new IllegalArgumentException("Build must be positive");
        }
    }

    public NucleusVersion(final String version) {
        var normalized = version;
        if (normalized.startsWith("v")) {
            normalized = normalized.substring(1);
        }

        final var parts = normalized.split("\\.", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Version must be in format 'year.month.build', got " + version);
        }
        final int year, month, build;
        try {
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
            build = Integer.parseInt(parts[2]);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Version must be in format 'year.month.build', got " + version);
        }

        this(year, month, build);
    }

    @Override
    public int compareTo(final NucleusVersion other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return this.year + "." + this.month + "." + this.build;
    }
}
