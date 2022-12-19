plugins {
    id("nucleus.base-conventions")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(projects.nucleusCommon)

    // Javacord
    implementation(libs.javacord.api)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.javacord.core)
    runtimeOnly(libs.slf4j.simple)
    runtimeOnly(libs.log4j.to.slf4j) // Javacord uses log4j

    implementation(libs.owner.java8)
    compileOnly(libs.auto.service.annotations)
    annotationProcessor(libs.auto.service.processor)
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.shadowJar {
    archiveFileName.set("NucleusDiscord.jar")
    manifest {
        attributes(
            "Main-Class" to "fr.xpdustry.nucleus.discord.NucleusBotLauncher",
            "Implementation-Title" to "NucleusDiscord",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Xpdustry"
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register<JavaExec>("runNucleusDiscord") {
    dependsOn(tasks.shadowJar)
    classpath(tasks.shadowJar)
    group = "nucleus"
    description = "Runs NucleusDiscord"
    mainClass.set("fr.xpdustry.nucleus.discord.NucleusBotLauncher")
}
