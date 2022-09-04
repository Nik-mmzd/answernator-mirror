plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.git.version) apply false
    alias(libs.plugins.shadow) apply false
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
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
    apply(plugin = rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    apply(plugin = rootProject.libs.plugins.kotlin.plugin.serialization.get().pluginId)
    apply(plugin = rootProject.libs.plugins.git.version.get().pluginId)

    repositories {
        mavenCentral()
    }

    dependencies {
        api(rootProject.libs.bundles.common)
        api(rootProject.libs.kotlin.logging)
        api(rootProject.libs.bundles.exposed)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "11"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "11"
        }
    }
}