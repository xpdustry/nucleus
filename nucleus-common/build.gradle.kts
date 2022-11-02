plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
}

dependencies {
    compileOnly("fr.xpdustry:javelin-core:${Versions.javelin}")
    compileOnly("org.slf4j:slf4j-api:${Versions.slf4j}")
    compileOnly("org.immutables:value:2.9.2")
    annotationProcessor("org.immutables:value:2.9.2")
}
