plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
}

dependencies {
    compileOnly("org.immutables:value:${Versions.immutables}")
    annotationProcessor("org.immutables:value:${Versions.immutables}")
}
