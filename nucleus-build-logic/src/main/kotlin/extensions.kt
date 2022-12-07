import org.gradle.api.Project
import java.time.Clock
import java.time.LocalDateTime

fun Project.getCalverVersion(): String {
    val today = LocalDateTime.now(Clock.systemUTC())
    val previous = project.file("VERSION.txt").readLines().first().split('.').map(String::toInt)
    val build = if (today.year == previous[0] && today.monthValue == previous[1]) previous[2] + 1 else 0
    return "${today.year}.${today.monthValue}.$build"
}