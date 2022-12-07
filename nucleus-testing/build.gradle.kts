import fr.xpdustry.toxopid.util.anukenJitpack
import fr.xpdustry.toxopid.util.mindustryDependencies

plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
    id("fr.xpdustry.toxopid")
}

toxopid {
    compileVersion.set(libs.versions.mindustry.map { "v$it" })
    platforms.set(setOf(fr.xpdustry.toxopid.ModPlatform.HEADLESS))
}

repositories {
    anukenJitpack()
}

dependencies {
    mindustryDependencies()
    compileOnly(libs.distributor.api)
}
