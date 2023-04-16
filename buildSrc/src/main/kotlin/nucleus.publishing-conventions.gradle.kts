plugins {
    id("net.kyori.indra.publishing")
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
}

indra {
    gpl3OnlyLicense()

    github("Xpdustry", "Nucleus") {
        ci(true)
        issues(true)
        scm(true)
    }

    configurePublications {
        pom {
            organization {
                name.set("Xpdustry")
                url.set("https://www.xpdustry.fr")
            }

            developers {
                developer {
                    id.set("Phinner")
                    timezone.set("Europe/Brussels")
                }

                developer {
                    id.set("ZetaMap")
                    timezone.set("Europe/Paris")
                }
            }
        }
    }
}

publishing {
    repositories {
        maven("https://maven.xpdustry.fr/releases") {
            name = "xpdustry"
            credentials(PasswordCredentials::class)
        }
    }
}

// Tricks to work around Kyori checks
tasks.requireClean {
    enabled = false
}