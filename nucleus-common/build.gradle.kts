plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
}

dependencies {
    api(libs.slf4j.api)
    api(libs.javelin.core)
    api(libs.mongodb.driver.sync)
    api(libs.password4j)
    api(libs.gson)
    api(libs.owner.java8)
    api(libs.guice)
    api(libs.kyori.event)
    api(libs.caffeine) {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
    api(libs.deepl) {
        exclude("org.jetbrains", "annotations")
    }
    api(libs.classgraph)
    api(libs.guava)
    compileOnly(libs.immutables.value.annotations)
    annotationProcessor(libs.immutables.value.processor)
    compileOnly(libs.javax.inject)
}
