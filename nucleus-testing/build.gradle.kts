import fr.xpdustry.toxopid.util.anukenJitpack
import fr.xpdustry.toxopid.util.mindustryDependencies

plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
    id("fr.xpdustry.toxopid")
}

toxopid {
    compileVersion.set("v${Versions.mindustry}")
    platforms.set(setOf(fr.xpdustry.toxopid.ModPlatform.HEADLESS))
}

repositories {
    anukenJitpack()
}

dependencies {
    mindustryDependencies()
    compileOnly("fr.xpdustry:distributor-api:${Versions.distributor}")
}
