import fr.xpdustry.toxopid.task.GitHubArtifact
import fr.xpdustry.toxopid.task.GitHubDownload
import fr.xpdustry.toxopid.util.anukenJitpack
import fr.xpdustry.toxopid.util.mindustryDependencies

plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
    id("com.github.johnrengelman.shadow")
    id("fr.xpdustry.toxopid")
}

val metadata = fr.xpdustry.toxopid.util.ModMetadata.fromJson(project.file("plugin.json"))
metadata.description = rootProject.description!!
metadata.version = rootProject.version.toString()

toxopid {
    compileVersion.set("v${metadata.minGameVersion}")
    platforms.add(fr.xpdustry.toxopid.ModPlatform.HEADLESS)
}

repositories {
    anukenJitpack()
    maven("https://maven.xpdustry.fr/snapshots")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots1"
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    api(project(":nucleus-common"))
    mindustryDependencies()
    // implementation("fr.xpdustry:javelin-core:1.0.0")
    compileOnly("fr.xpdustry:javelin-mindustry:1.0.0")
    compileOnly("fr.xpdustry:distributor-api:3.0.0-rc.2-SNAPSHOT")
}

tasks.shadowJar {
    doFirst {
        val temp = temporaryDir.resolve("plugin.json")
        temp.writeText(metadata.toJson(true))
        from(temp)
    }
    from(rootProject.file("LICENSE.md")) {
        into("META-INF")
    }
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

val javelin = tasks.register<GitHubDownload>("downloadJavelin") {
    artifacts.addAll(
        GitHubArtifact.release(
            "Xpdustry",
            "Javelin",
            "v1.0.0",
            "Javelin.jar"
        )
    )
}

tasks.runMindustryClient {
    mods.setFrom()
}

tasks.runMindustryServer {
    mods.setFrom(tasks.shadowJar, javelin, rootProject.file("libs/distributor-core-3.0.0-rc.2-SNAPSHOT-all.jar"))
}
