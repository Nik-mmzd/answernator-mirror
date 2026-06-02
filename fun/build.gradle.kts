plugins {
    alias(libs.plugins.buildconfig)
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder.answernator"
version = gitVersion(mapOf ("prefix" to "fun@"))

dependencies {
    implementation(project(":Answernator"))
}

buildConfig {
    packageName("pw.modder.answernator.fun")
    className("BuildConfig")

    buildConfigField("APP_VERSION", project.version.toString())
}
