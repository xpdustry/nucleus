import fr.xpdustry.toxopid.dsl.anukenJitpack
import fr.xpdustry.toxopid.dsl.mindustryDependencies
import fr.xpdustry.toxopid.task.GithubArtifactDownload
import fr.xpdustry.toxopid.task.MindustryExec

plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
    id("com.github.johnrengelman.shadow")
    id("fr.xpdustry.toxopid")
}

val metadata = fr.xpdustry.toxopid.spec.ModMetadata.fromJson(project.file("plugin.json"))
metadata.minGameVersion = libs.versions.mindustry.get()
metadata.description = rootProject.description!!
metadata.version = rootProject.version.toString()

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
    api(projects.nucleusCommon) {
        exclude("org.slf4j", "slf4j-api") // Provided by Distributor
        exclude("fr.xpdustry", "javelin-core") // Provided by JavelinPlugin
    }
    compileOnly(libs.distributor.api)
    compileOnly(libs.javelin.mindustry)
    implementation(libs.owner.java8)
    implementation(libs.gson)
    implementation(libs.prettytime)
    implementation(libs.time4j.core)
    implementation(libs.configurate.core) {
        exclude("io.leangen.geantyref", "geantyref") // Provided by Distributor
    }
    implementation(libs.configurate.yaml) {
        exclude("io.leangen.geantyref", "geantyref") // Provided by Distributor
    }
    implementation(libs.expressible) {
        exclude("org.jetbrains", "annotations")
    }
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

    val shadowPackage = "fr.xpdustry.nucleus.mindustry.shadow"
    relocate("com.google.gson", "$shadowPackage.gson")
    relocate("com.google.common", "$shadowPackage.common")
    relocate("com.google.inject", "$shadowPackage.inject")
    relocate("com.google.thirdparty.publicsuffix", "$shadowPackage.publicsuffix")
    relocate("org.aeonbits.owner", "$shadowPackage.owner")
    relocate("org.bson", "$shadowPackage.bson")
    relocate("com.mongodb", "$shadowPackage.mongodb")
    relocate("com.password4j", "$shadowPackage.password4j")
    relocate("com.deepl.api", "$shadowPackage.deepl")
    relocate("org.ocpsoft.prettytime", "$shadowPackage.prettytime")
    relocate("fr.xpdustry.nucleus.mindustry.testing", "$shadowPackage.testing")
    relocate("org.spongepowered.configurate", "$shadowPackage.configurate")
    relocate("org.yaml.snakeyaml", "$shadowPackage.snakeyaml")
    relocate("com.github.benmanes.caffeine", "$shadowPackage.caffeine")
    relocate("io.github.classgraph", "$shadowPackage.classgraph")
    relocate("javax.inject", "$shadowPackage.javax.inject")
    relocate("net.kyori.event", "$shadowPackage.event")
    relocate("nonapi.io.github.classgraph", "$shadowPackage.nonapi.classgraph")
    relocate("org.aopalliance", "$shadowPackage.aopalliance")
    relocate("panda", "$shadowPackage.panda")
    relocate("assets", "$shadowPackage.assets")
    relocate("data", "$shadowPackage.data")
    relocate("net.time4j", "$shadowPackage.time4j")

    minimize {
        exclude(dependency("org.ocpsoft.prettytime:prettytime:.*"))
        exclude(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
        exclude(dependency("net.time4j:time4j-core:.*"))
    }

    mergeServiceFiles()
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
    name.set("DistributorCore.jar")
    version.set(libs.versions.distributor.map { "v$it" })
}

tasks.runMindustryClient {
    mods.setFrom()
}

tasks.runMindustryServer {
    mods.setFrom(tasks.shadowJar, downloadJavelin, downloadDistributor)
}

// Second server for testing discovery
tasks.register<MindustryExec>("runMindustryServer2") {
    group = fr.xpdustry.toxopid.Toxopid.TASK_GROUP_NAME
    classpath(tasks.downloadMindustryServer)
    mainClass.set("mindustry.server.ServerLauncher")
    modsPath.set("./config/mods")
    standardInput = System.`in`
    mods.setFrom(tasks.shadowJar, downloadJavelin, downloadDistributor)
}
