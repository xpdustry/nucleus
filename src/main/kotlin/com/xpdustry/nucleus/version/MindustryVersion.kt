// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.version

/** A Mindustry version. */
data class MindustryVersion(val major: Int, val build: Int, val patch: Int, val type: Type) {
    override fun toString(): String {
        val name = this.type.name.lowercase().replace('_', '-')
        return name + " v" + this.major + " " + this.build + (if (this.patch == 0) "" else "." + this.patch)
    }

    /** A Mindustry release channel. */
    enum class Type {
        OFFICIAL,
        ALPHA,
        BLEEDING_EDGE,
        CUSTOM,
    }

    init {
        require(major >= 0) { "Major version must be positive" }
        require(build >= 0) { "Build version must be positive" }
        require(patch >= 0) { "Patch version must be positive" }
    }
}
