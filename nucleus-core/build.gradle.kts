plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
}

dependencies {
    compileOnly(libs.immutables.value.annotations)
    annotationProcessor(libs.immutables.value.processor)

    api(libs.slf4j.api)
    api(libs.javelin.core)
    api(libs.mongodb.driver.sync)
    api(libs.password4j)
    api(libs.deepl) {
        exclude("org.jetbrains", "annotations")
    }
}
