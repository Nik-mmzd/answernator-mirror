plugins {
    `maven-publish`
    `version-catalog`
    application
    alias(libs.plugins.buildconfig)
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder"
version = gitVersion()

catalog {
    versionCatalog {
        from(files("../gradle/libs.versions.toml"))
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

val slf4jVersion: String by project
dependencies {
    implementation(libs.guava)
    implementation(libs.apache.commons.io)
    implementation(libs.bundles.sentry.kotlin)
    implementation(libs.kodein.di)
    runtimeOnly(libs.h2)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.kotlin.test)
}

application {
    applicationName = "Answernator"
    mainClass = "pw.modder.answernator4.MainKt"
}

java {
    withSourcesJar()
}

tasks {
    test {
        useJUnitPlatform()
        failOnNoDiscoveredTests = false
    }

    distTar {
        compression = Compression.GZIP
        archiveVersion.convention(null as String?)
    }

    distZip {
        archiveVersion.convention(null as String?)
    }
}

buildConfig {
    className("BuildConfig")
    packageName("pw.modder.answernator4")

    buildConfigField("APP_VERSION", project.version.toString())
    buildConfigField("APP_NAME", project.name)
    buildConfigField("APP_CREATOR_ID", providers.gradleProperty("answernator.author.id"))
    buildConfigField("APP_SOURCE_URL", providers.gradleProperty("answernator.url.source"))
    buildConfigField("APP_ISSUES_URL", providers.gradleProperty("answernator.url.issues"))
    buildConfigField("KORD_VERSION", libs.versions.kord)
}
