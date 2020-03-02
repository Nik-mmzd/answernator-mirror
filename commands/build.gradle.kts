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
    implementation(project(":"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jessecorbett:diskord-jvm:1.5.3")
    implementation("commons-io:commons-io:2.6")
    implementation("io.github.microutils:kotlin-logging:1.7.8")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}