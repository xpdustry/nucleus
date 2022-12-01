import fr.xpdustry.toxopid.task.ModArtifactDownload
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
    sonatype.s01Snapshots()
}

dependencies {
    api(project(":nucleus-common"))
    mindustryDependencies()
    compileOnly("fr.xpdustry:distributor-api:${Versions.distributor}")
    compileOnly("fr.xpdustry:javelin-mindustry:${Versions.javelin}")
    implementation("org.aeonbits.owner:owner-java8:${Versions.owner}")
    implementation("com.google.code.gson:gson:${Versions.gson}")
    compileOnly("org.immutables:value:${Versions.immutables}")
    annotationProcessor("org.immutables:value:${Versions.immutables}")
}

tasks.shadowJar {
    archiveFileName.set("NucleusMindustry.jar")
    archiveClassifier.set("plugin")
    doFirst {
        val temp = temporaryDir.resolve("plugin.json")
        temp.writeText(metadata.toJson(true))
        from(temp)
    }
    from(rootProject.file("LICENSE.md")) {
        into("META-INF")
    }
    relocate("com.google.gson", "fr.xpdustry.nucleus.shadow.gson")
    relocate("org.aeonbits.owner", "fr.xpdustry.nucleus.shadow.owner")
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

val downloadJavelin = tasks.register<ModArtifactDownload>("downloadJavelin") {
    user.set("Xpdustry")
    repo.set("Javelin")
    version.set("v${Versions.javelin}")
}

val downloadDistributor = tasks.register<ModArtifactDownload>("downloadDistributor") {
    user.set("Xpdustry")
    repo.set("Distributor")
    version.set("v${Versions.distributor}")
}

tasks.runMindustryClient {
    mods.setFrom()
}

tasks.runMindustryServer {
    mods.setFrom(tasks.shadowJar, downloadJavelin, downloadDistributor)
}
