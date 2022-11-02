plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":nucleus-common"))
    implementation("org.javacord:javacord:3.6.0")
    implementation("cloud.commandframework:cloud-core:${Versions.cloud}")
    implementation("cloud.commandframework:cloud-annotations:${Versions.cloud}")
    implementation("cloud.commandframework:cloud-javacord:${Versions.cloud}")
    implementation("org.aeonbits.owner:owner-java8:${Versions.owner}")
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    implementation("fr.xpdustry:javelin-core:${Versions.javelin}")
    runtimeOnly("org.slf4j:slf4j-simple:${Versions.slf4j}")
    runtimeOnly("org.apache.logging.log4j:log4j-to-slf4j:2.19.0")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "fr.xpdustry.nucleus.discord.NucleusBotBootstrap"
    }
    from(rootProject.file("LICENSE.md")) {
        into("META-INF")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
