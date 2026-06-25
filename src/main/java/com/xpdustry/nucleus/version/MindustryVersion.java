// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.version;

import java.util.Locale;
import java.util.Objects;

/// A Mindustry version.
public record MindustryVersion(int major, int build, int patch, Type type) {

    public MindustryVersion {
        if (major < 0) {
            throw new IllegalArgumentException("Major version must be positive");
        }
        if (build < 0) {
            throw new IllegalArgumentException("Build version must be positive");
        }
        if (patch < 0) {
            throw new IllegalArgumentException("Patch version must be positive");
        }
        Objects.requireNonNull(type, "type");
    }

    @Override
    public String toString() {
        final var name = this.type.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return name + " v" + this.major + " " + this.build + (this.patch == 0 ? "" : "." + this.patch);
    }

    /// A Mindustry release channel.
    public enum Type {
        OFFICIAL,
        ALPHA,
        BLEEDING_EDGE,
        CUSTOM,
    }
}
