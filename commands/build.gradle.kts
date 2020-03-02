plugins {
    kotlin("jvm")
    id("kotlinx-serialization")
}

group = "pw.modder.answernator"
version = "3.0.0-aplha0.dirty"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jessecorbett:diskord-jvm:1.5.3")
    implementation("commons-io:commons-io:2.6")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}