plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.xpdustry.fr/snapshots/") {
        name = "xpdustry-snapshots"
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    implementation(libs.indra.licenser.spotless)
    implementation(libs.indra.common)
    implementation(libs.toxopid)
    implementation(libs.spotless)
    implementation(libs.shadow)
    implementation(libs.errorprone.gradle)
    // https://github.com/KyoriPowered/adventure/blob/b271c100a463a5bdc850753d571bf555ef855b85/build-logic/build.gradle.kts#L17
    compileOnly(files(libs::class.java.protectionDomain.codeSource.location))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
