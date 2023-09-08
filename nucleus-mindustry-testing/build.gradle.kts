import fr.xpdustry.toxopid.dsl.mindustryDependencies

plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
    id("fr.xpdustry.toxopid")
}

toxopid {
    compileVersion.set(libs.versions.mindustry.map { "v$it" })
    platforms.set(setOf(fr.xpdustry.toxopid.spec.ModPlatform.HEADLESS))
}

repositories {
    maven("https://maven.xpdustry.fr/mindustry") {
        name = "xpdustry-mindustry"
        mavenContent { releasesOnly() }
    }
}

dependencies {
    mindustryDependencies()
    compileOnly(libs.distributor.api)
}
