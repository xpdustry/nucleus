enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    @Suppress("UnstableApiUsage")
    includeBuild("nucleus-build-logic")
}

rootProject.name = "nucleus-parent"

include("nucleus-core")
include("nucleus-discord")
include("nucleus-mindustry")
include("nucleus-mindustry-testing")
