import java.time.Clock
import java.time.LocalDateTime

plugins {
    id("nucleus.parent-conventions")
}

group = "fr.xpdustry"
description = "The software collection powering Xpdustry."
version = file("VERSION.txt").readLines().first()

tasks.register("incrementVersionFile") {
    doLast {
        val today = LocalDateTime.now(Clock.systemUTC())
        val previous = version.toString().split('.').map(String::toInt)
        val build = if (today.year == previous[0] && today.monthValue == previous[1]) previous[2] + 1 else 0
        file("VERSION.txt").writeText("${today.year}.${today.monthValue}.$build")
    }
}
