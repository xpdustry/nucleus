plugins {
    id("nucleus.parent-conventions")
}

group = "fr.xpdustry"
description = "The core of the Xpdustry network."
version = findProperty("releaseVersion") ?: project.getCalverVersion()
