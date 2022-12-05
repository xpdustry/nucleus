plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
}

dependencies {
    // TODO Isolate javelin as an implementation detail with a messenger API
    compileOnly("fr.xpdustry:javelin-core:${Versions.javelin}")
    // TODO Investigate google/auto as a replacement for immutables
    compileOnly("org.immutables:value:${Versions.immutables}")
    annotationProcessor("org.immutables:value:${Versions.immutables}")
}
