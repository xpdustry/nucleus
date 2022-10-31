plugins {
    id("nucleus.parent-conventions")
}

group = "fr.xpdustry"
description = "The software collection powering Xpdustry."
version = "1.0.0" + if (indraGit.headTag() == null) "-SNAPSHOT" else ""
