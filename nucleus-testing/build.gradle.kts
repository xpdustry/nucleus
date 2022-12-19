import fr.xpdustry.toxopid.dsl.anukenJitpack
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
    anukenJitpack()
}

dependencies {
    mindustryDependencies()
    compileOnly(libs.distributor.api)
}
