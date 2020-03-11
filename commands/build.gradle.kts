import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("kotlinx-serialization")
    id("com.palantir.git-version")
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder.answernator"
version = gitVersion(mapOf ("prefix" to "cmd@"))

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Answernator"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jessecorbett:diskord-jvm:1.5.3")
    implementation("commons-io:commons-io:2.6")
    implementation("io.github.microutils:kotlin-logging:1.7.8")
    implementation("io.ktor:ktor-client-core:1.2.6")
    implementation("io.ktor:ktor-client-core-jvm:1.2.6")
    implementation("io.ktor:ktor-client-cio:1.2.6")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    val createModuleVersionFile by creating {
        doLast {
            file("$buildDir/module.txt").printWriter().use { pw ->
                pw.append(project.version.toString())
                pw.appendln()
            }
        }
    }

    jar {
        dependsOn(createModuleVersionFile)
        from("$buildDir/module.txt")
    }
}