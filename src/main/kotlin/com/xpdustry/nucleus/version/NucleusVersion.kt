// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.version

/** A calendar-based version for nucleus. */
data class NucleusVersion(val year: Int, val month: Int, val build: Int) : Comparable<NucleusVersion> {
    init {
        require(year >= 0) { "Year must be positive" }
        require(month in 1..12) { "Month must be between 1 and 12" }
        require(build >= 0) { "Build must be positive" }
    }

    override fun compareTo(other: NucleusVersion): Int {
        return COMPARATOR.compare(this, other)
    }

    override fun toString(): String {
        return this.year.toString() + "." + this.month + "." + this.build
    }

    companion object {
        private val COMPARATOR = compareBy(NucleusVersion::year, NucleusVersion::month, NucleusVersion::build)

        operator fun invoke(version: String): NucleusVersion {
            val parts = version.trimStart('v').split('.')
            require(parts.size == 3) { "Version must be in format 'year.month.build', got $version" }
            try {
                val (year, month, build) = parts.map(String::toInt)
                return NucleusVersion(version)
            } catch (e: NumberFormatException) {
                error("Version must be in format 'year.month.build', got $version")
            }
        }
    }
}
