plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
}

dependencies {
    api(projects.nucleusApi)
    api(libs.slf4j.api)
    api(libs.javelin.core)
    api(libs.mongodb.driver.sync)
    api(libs.password4j)
    api(libs.deepl)
}
