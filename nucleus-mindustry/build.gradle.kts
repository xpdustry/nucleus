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
    maven("https://maven.xpdustry.fr/releases")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots1"
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    api(project(":nucleus-common"))
    mindustryDependencies()
    compileOnly("fr.xpdustry:javelin-mindustry:${Versions.javelin}")
    compileOnly("fr.xpdustry:distributor-api:${Versions.distributor}")
    implementation("org.aeonbits.owner:owner-java8:${Versions.owner}")
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

val pluginDependencies = tasks.register<GitHubDownload>("downloadPluginDependencies") {
    artifacts.addAll(
        GitHubArtifact.release(
            "Xpdustry",
            "Javelin",
            "v" + Versions.javelin,
            "Javelin.jar"
        ),
        GitHubArtifact.release(
            "Xpdustry",
            "Distributor",
            "v" + Versions.distributor,
            "Distributor.jar"
        )
    )
}

tasks.runMindustryClient {
    mods.setFrom()
}

tasks.runMindustryServer {
    mods.setFrom(tasks.shadowJar, pluginDependencies)
}
