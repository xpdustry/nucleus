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

    implementation("org.aeonbits.owner:owner:1.0.12")
    implementation("com.google.inject:guice:5.1.0")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.shadowJar {
    archiveFileName.set("NucleusDiscord.jar")
}
