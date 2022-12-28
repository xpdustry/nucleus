import fr.xpdustry.toxopid.dsl.anukenJitpack
import fr.xpdustry.toxopid.dsl.mindustryDependencies
import fr.xpdustry.toxopid.task.GithubArtifactDownload

plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
    id("com.github.johnrengelman.shadow")
    id("fr.xpdustry.toxopid")
}

val metadata = fr.xpdustry.toxopid.spec.ModMetadata.fromJson(project.file("plugin.json"))
metadata.minGameVersion = libs.versions.mindustry.get()
metadata.description = rootProject.description!!
metadata.version = "v" + rootProject.version.toString()

toxopid {
    compileVersion.set(libs.versions.mindustry.map { "v$it" })
    platforms.add(fr.xpdustry.toxopid.spec.ModPlatform.HEADLESS)
}

repositories {
    anukenJitpack()
}

dependencies {
    mindustryDependencies()
    implementation(projects.nucleusMindustryTesting)
    api(projects.nucleusCore) {
        exclude("org.slf4j", "slf4j-api") // Provided by Distributor
        exclude("fr.xpdustry", "javelin-core") // Provided by JavelinPlugin
    }
    compileOnly(libs.distributor.api)
    compileOnly(libs.javelin.mindustry)
    implementation(libs.owner.java8)
    implementation(libs.gson)
    implementation(libs.prettytime)
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
    relocate("com.deepl.api", "fr.xpdustry.nucleus.shadow.deepl")
    relocate("org.ocpsoft.prettytime", "fr.xpdustry.nucleus.shadow.prettytime")
    relocate("fr.xpdustry.nucleus.mindustry.testing", "fr.xpdustry.nucleus.shadow.testing")
    minimize {
        exclude(dependency("org.ocpsoft.prettytime:prettytime:.*"))
    }
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

val downloadJavelin = tasks.register<GithubArtifactDownload>("downloadJavelin") {
    user.set("Xpdustry")
    repo.set("Javelin")
    name.set("Javelin.jar")
    version.set(libs.versions.javelin.map { "v$it" })
}

val downloadDistributor = tasks.register<GithubArtifactDownload>("downloadDistributor") {
    user.set("Xpdustry")
    repo.set("Distributor")
    name.set("Distributor.jar")
    version.set(libs.versions.distributor.map { "v$it" })
}

tasks.runMindustryClient {
    mods.setFrom()
}

tasks.runMindustryServer {
    mods.setFrom(tasks.shadowJar, downloadJavelin, downloadDistributor)
}
