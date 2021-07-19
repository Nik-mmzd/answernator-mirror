plugins {
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21" apply false
    id("com.palantir.git-version") version "0.12.2" apply false
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

val exposedVersion: String by project
val ktorVersion: String by project
val commonsIoVersion: String by project
val kotlinLoggingVersion: String by project
val guavaVersion: String by project
val h2Version: String by project
val kordVersion: String by project

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))
        implementation("dev.kord:kord-core:$kordVersion")
        implementation("commons-io:commons-io:$commonsIoVersion")
        implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
        implementation("com.google.guava:guava:$guavaVersion")
        implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
        implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
        implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
        implementation("io.ktor:ktor-client-core:$ktorVersion")
        implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("com.h2database:h2:$h2Version")
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}