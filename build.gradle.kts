plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.git.version) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.buildconfig) apply false
}

allprojects {
    repositories {
        mavenCentral()
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

    kotlin.jvmToolchain(17)
    java.toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}
