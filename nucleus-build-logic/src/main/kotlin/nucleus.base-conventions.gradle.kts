import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless")
    id("net.kyori.indra")
    id("net.kyori.indra.licenser.spotless")
    id("net.ltgt.errorprone")
}

// expose version catalog
val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

repositories {
    mavenCentral()
    maven("https://maven.xpdustry.fr/releases") {
        name = "xpdustry-repository-releases"
        mavenContent { releasesOnly() }
    }
    sonatype.s01Snapshots()
}

dependencies {
    compileOnly(libs.checkerframework)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)

    // Static analysis
    annotationProcessor(libs.nullaway)
    errorprone(libs.errorprone.core)
}

indra {
    javaVersions {
        target(17)
        minimumToolchain(17)
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER.md"))
}

spotless {
    java {
        palantirJavaFormat()
        formatAnnotations()
        importOrderFile(rootProject.file(".spotless/nucleus.importorder"))
    }
}

tasks.withType<JavaCompile> {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        disable(
            "MissingSummary",
            "BadImport",
            "FutureReturnValueIgnored",
            "InlineMeSuggester",
            "EmptyCatch",
        )
        if (!name.contains("test", true)) {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "fr.xpdustry.nucleus")
            option("NullAway:TreatGeneratedAsUnannotated", true)
        }
    }
}
