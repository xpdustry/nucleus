enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "nucleus-build-logic"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    versionCatalogs {
        register("libs") {
            from(files("../gradle/libs.versions.toml")) // include from parent project
        }
    }
}
