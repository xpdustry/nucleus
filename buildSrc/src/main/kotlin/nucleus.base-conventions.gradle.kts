import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless")
    id("net.kyori.indra")
    id("net.kyori.indra.licenser.spotless")
    id("net.ltgt.errorprone")
}

repositories {
    mavenCentral()
    maven("https://maven.xpdustry.fr/releases") {
        name = "xpdustry-repository-releases"
        mavenContent { releasesOnly() }
    }
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots1"
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    compileOnly("org.checkerframework:checker-qual:3.26.0")

    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junit}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")

    // Static analysis
    annotationProcessor("com.uber.nullaway:nullaway:0.9.4")
    errorprone("com.google.errorprone:error_prone_core:2.10.0")
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
