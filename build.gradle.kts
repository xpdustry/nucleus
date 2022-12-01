import java.time.Clock
import java.time.LocalDateTime

plugins {
    id("nucleus.parent-conventions")
}

group = "fr.xpdustry"
description = "The core of the Xpdustry network."

val today = LocalDateTime.now(Clock.systemUTC())
val previous = file("VERSION.txt").readLines().first().split('.').map(String::toInt)
val build = if (today.year == previous[0] && today.monthValue == previous[1]) previous[2] + 1 else 0
version = "${today.year}.${today.monthValue}.$build"

tasks.register("incrementVersionFile") {
    doLast {file("VERSION.txt").writeText(version.toString()) }
}
