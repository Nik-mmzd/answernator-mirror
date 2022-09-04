plugins {
    id(libs.plugins.shadow.get().pluginId)
    `maven-publish`
    `version-catalog`
}

catalog {
    versionCatalog {
        from(files("gradle/libs.versions.toml"))
        version("answernator", project.version.toString())
        library("answernator", project.group.toString(), "answernator").versionRef("answernator")
    }
}


publishing {
    publications {
        create<MavenPublication>("answernator") {
            artifactId = "answernator"

            from(components["java"])
        }

        create<MavenPublication>("catalog") {
            artifactId = "answernator-catalog"
            groupId = project.group.toString() + ".catalogs"

            from(components["versionCatalog"])
        }
    }

    repositories {
        maven {
            name = "gitlab.modder.pw"
            url = uri("${System.getenv("CI_API_V4_URL")}/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")

            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder"
version = gitVersion()

val slf4jVersion: String by project
dependencies {
    implementation(libs.bundles.exposed)
    implementation(libs.guava)
    implementation(libs.apache.commons.io)
    runtimeOnly(libs.h2)
    runtimeOnly(libs.logback.classic)
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "pw.modder.answernator.MainKt"
    }
}

java {
    withSourcesJar()
}

tasks {
    val createDependenciesFile by creating {
        doLast {
            file("$buildDir/dependencies.txt").printWriter().use { pw ->
                pw.appendLine("${project.group}:${project.name}:${project.version}")
                configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts.forEach {
                    pw.appendLine(it.moduleVersion.toString())
                }
            }
        }
    }

    jar {
        dependsOn(createDependenciesFile)
        from("$buildDir/dependencies.txt")
    }

    shadowJar {
        dependsOn(createDependenciesFile)
        from("$buildDir/dependencies.txt")
    }
}