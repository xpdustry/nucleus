plugins {
    id("nucleus.base-conventions")
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
}

dependencies {
    implementation(projects.nucleusCommon)

    // Javacord
    implementation(libs.javacord.api)
    runtimeOnly(libs.javacord.core)
    runtimeOnly(libs.log4j.to.slf4j) // Javacord uses log4j

    // Spring
    implementation("org.springframework.boot:spring-boot-starter")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.bootJar.get().archiveFile.get().toString()) }
}

tasks.bootJar {
    archiveBaseName.set("NucleusDiscord")
}