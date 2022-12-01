import java.time.Clock
import java.time.LocalDateTime

tasks.register("incrementVersionFile") {
    doLast {file("VERSION.txt").writeText(project.getCalverVersion()) }
}
