plugins {
    id("nucleus.base-conventions")
    id("nucleus.publishing-conventions")
}

dependencies {
    compileOnly("fr.xpdustry:javelin-core:${Versions.javelin}")
    api(project(":nucleus-api"))
    api("org.mongodb:mongodb-driver-sync:${Versions.mongodb}") {
        exclude("org.slf4j", "slf4j-api") // Provided by Distributor
    }
    api("com.password4j:password4j:${Versions.password4j}") {
        exclude("org.slf4j", "slf4j-api") // Provided by Distributor
    }
}
