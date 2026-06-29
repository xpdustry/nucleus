import com.xpdustry.toxopid.extension.anukeXpdustry
import com.xpdustry.toxopid.spec.ModDependency
import com.xpdustry.toxopid.spec.ModMetadata
import com.xpdustry.toxopid.spec.ModPlatform
import com.xpdustry.toxopid.task.GithubAssetDownload
import com.xpdustry.toxopid.task.MindustryExec
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

plugins {
    id("com.diffplug.spotless") version "8.7.0"
    id("net.kyori.indra") version "4.0.0"
    id("com.gradleup.shadow") version "9.4.1"
    id("com.xpdustry.toxopid") version "4.2.0"
    id("net.ltgt.errorprone") version "5.1.0"
}

val metadata = ModMetadata()
metadata.name = "xpdustry-nucleus"
metadata.displayName = "Nucleus"
metadata.version = computeNextVersion()
metadata.author = "xpdustry"
metadata.mainClass = "com.xpdustry.nucleus.NucleusPlugin"
metadata.minGameVersion = "158"
metadata.description = "The core plugin of xpdustry."
metadata.hidden = true
metadata.dependencies +=
    arrayOf(
        ModDependency("nohorny"),
        ModDependency("slf4md"),
        ModDependency("sql4md-postgresql"),
        ModDependency("sql4md-postgresql-embedded", soft = true),
    )

group = "com.xpdustry"
version = metadata.version
description = metadata.description

fun computeNextVersion(): String {
    val parts =
        rootProject
            .file("VERSION.txt")
            .readText()
            .split('.', limit = 3)
            .map(String::toInt)
    require(parts.size == 3) {
        "Invalid version format: $parts"
    }

    var (year, month, build) = parts

    if (findProperty("is_release").toString().toBoolean()) {
        val timestamp =
            ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(property("build_timestamp").toString().toLong()),
                ZoneOffset.UTC,
            )
        if (timestamp.year == year && timestamp.monthValue == month) {
            build += 1
        } else {
            year = timestamp.year
            month = timestamp.monthValue
            build = 0
        }
    } else {
        build += 1
    }

    return "$year.$month.$build"
}

repositories {
    mavenCentral()
    anukeXpdustry()
}

toxopid {
    compileVersion = "v" + metadata.minGameVersion
    platforms = setOf(ModPlatform.SERVER)
}

dependencies {
    compileOnly(toxopid.dependencies.mindustryCore)
    compileOnly(toxopid.dependencies.mindustryHeadless)
    testImplementation(toxopid.dependencies.mindustryCore)
    testImplementation(toxopid.dependencies.mindustryHeadless)

    compileOnly(toxopid.dependencies.arcCore)
    compileOnly(toxopid.dependencies.arcHeadless)
    testImplementation(toxopid.dependencies.arcCore)
    testImplementation(toxopid.dependencies.arcHeadless)

    implementation("com.google.code.gson:gson:2.14.0")

    compileOnly("org.slf4j:slf4j-api:2.0.18")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.18")

    compileOnly("org.postgresql:postgresql:42.7.11")
    testImplementation("org.postgresql:postgresql:42.7.11")
    compileOnly("io.zonky.test:embedded-postgres:2.2.2")
    testImplementation("io.zonky.test:embedded-postgres:2.2.2")
    implementation("com.zaxxer:HikariCP:7.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testImplementation("org.junit.vintage:junit-vintage-engine:6.0.1")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("com.google.guava:guava-testlib:33.4.8-jre")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnlyApi("org.jetbrains:annotations:26.1.0")
    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    annotationProcessor("com.uber.nullaway:nullaway:0.13.4")
    testAnnotationProcessor("com.uber.nullaway:nullaway:0.13.4")
    errorprone("com.google.errorprone:error_prone_core:2.49.0")
}

configurations.runtimeClasspath {
    exclude("org.slf4j")
    exclude("com.google.errorprone")
}

indra {
    javaVersions {
        target(25)
        minimumToolchain(25)
    }

    publishSnapshotsTo("xpdustry", "https://maven.xpdustry.com/snapshots")
    publishReleasesTo("xpdustry", "https://maven.xpdustry.com/releases")

    gpl3OnlyLicense()

    if (metadata.repository.isNotBlank()) {
        val repo = metadata.repository.split("/")
        github(repo[0], repo[1]) {
            ci(true)
            issues(true)
            scm(true)
        }
    }

    configurePublications {
        pom {
            organization {
                name = "xpdustry"
                url = "https://www.xpdustry.com"
            }

            developers {
                developer {
                    id = "phinner"
                    timezone = "Europe/Brussels"
                }
            }
        }
    }
}

spotless {
    java {
        palantirJavaFormat("2.94.0")
        formatAnnotations()
        importOrder("", "\\#")
        forbidModuleImports()
        forbidWildcardImports()
        licenseHeader("// SPDX-License-Identifier: GPL-3.0-only")
    }
    kotlinGradle {
        ktlint()
    }
}

val generateMetadataFile =
    tasks.register("generateMetadataFile") {
        inputs.property("metadata", metadata)
        val output = layout.buildDirectory.file("plugin.json")
        outputs.file(output)
        doLast { output.get().asFile.writeText(ModMetadata.toJson(metadata)) }
    }

tasks.shadowJar {
    archiveFileName = "${project.name}.jar"
    archiveClassifier = "plugin"
    from(generateMetadataFile)
    from(rootProject.file("LICENSE.md")) { into("META-INF") }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile> {
    options.errorprone {
        disable("MissingSummary", "InlineMeSuggester")
        option("NullAway:OnlyNullMarked")
        option("NullAway:JSpecifyMode", "true")
        check("NullAway", CheckSeverity.ERROR)
    }
}

tasks.withType<MindustryExec> {
    jvmArguments.add("--enable-native-access=ALL-UNNAMED")
}

val downloadSlf4md =
    tasks.register<GithubAssetDownload>("downloadSlf4md") {
        owner = "xpdustry"
        repo = "slf4md"
        asset = "slf4md.jar"
        version = "v1.3.0"
    }

val downloadSql4mdPostgresql =
    tasks.register<GithubAssetDownload>("downloadSql4mdPostgresql") {
        owner = "xpdustry"
        repo = "sql4md"
        asset = "sql4md-postgresql.jar"
        version = "v2.1.0"
    }

val downloadSql4mdPostgresqlEmbedded =
    tasks.register<GithubAssetDownload>("downloadSql4mdPostgresqlEmbedded") {
        owner = "xpdustry"
        repo = "sql4md"
        asset = "sql4md-postgresql-embedded.jar"
        version = "v2.1.0"
    }

val downloadNoHorny =
    tasks.register<GithubAssetDownload>("downloadNoHorny") {
        owner = "xpdustry"
        repo = "nohorny"
        asset = "nohorny-client.jar"
        version = "v4.0.0-beta.7"
    }

tasks.runMindustryServer {
    mods.from(downloadSlf4md, downloadSql4mdPostgresql, downloadSql4mdPostgresqlEmbedded, downloadNoHorny)
}
