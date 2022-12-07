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
metadata.minGameVersion = libs.versions.mindustry.get()
metadata.description = rootProject.description!!
metadata.version = rootProject.version.toString()

toxopid {
    compileVersion.set(libs.versions.mindustry.map { "v$it" })
    platforms.add(fr.xpdustry.toxopid.ModPlatform.HEADLESS)
}

repositories {
    anukenJitpack()
}

dependencies {
    mindustryDependencies()
    api(projects.nucleusTesting)
    api(projects.nucleusCommon) {
        exclude("org.slf4j", "slf4j-api")       // Provided by Distributor
        exclude("fr.xpdustry", "javelin-core")  // Provided by JavelinPlugin
    }
    compileOnly(libs.distributor.api)
    compileOnly(libs.javelin.mindustry)
    implementation(libs.owner.java8)
    implementation(libs.gson)
    compileOnly(libs.immutables.value.annotations)
    annotationProcessor(libs.immutables.value.processor)
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
    relocate("org.bson", "fr.xpdustry.nucleus.shadow.bson")
    relocate("com.mongodb", "fr.xpdustry.nucleus.shadow.mongodb")
    relocate("com.password4j", "fr.xpdustry.nucleus.shadow.password4j")
    minimize()
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
    version.set(libs.versions.javelin)
}

val downloadDistributor = tasks.register<ModArtifactDownload>("downloadDistributor") {
    user.set("Xpdustry")
    repo.set("Distributor")
    version.set(libs.versions.distributor)
}

tasks.runMindustryClient {
    mods.setFrom()
}

tasks.runMindustryServer {
    mods.setFrom(tasks.shadowJar, downloadJavelin, downloadDistributor)
}
