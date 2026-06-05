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
        // Patched Kord fork published to the answernator GitLab Maven registry.
        // Scoped to only the forked modules; everything else (incl. dev.kord:kord-cache-*) stays on Maven Central.
        maven {
            name = "gitlab-local"
            url = uri("https://gitlab.modder.pw/api/v4/groups/answernator/-/packages/maven")
            content {
                includeGroup("dev.kord")
            }
        }
    }
}

subprojects {
    apply(plugin = rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    apply(plugin = rootProject.libs.plugins.kotlin.plugin.serialization.get().pluginId)
    apply(plugin = rootProject.libs.plugins.git.version.get().pluginId)

    repositories {
        mavenCentral()
    }

    kotlin.jvmToolchain(17)
    java.toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}
