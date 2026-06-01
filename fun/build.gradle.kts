plugins {
    alias(libs.plugins.buildconfig)
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder.answernator"
version = gitVersion(mapOf ("prefix" to "fun@"))

dependencies {
    implementation(project(":Answernator"))
    implementation(libs.kodein.di)
}

buildConfig {
    packageName("pw.modder.answernator.fun")
    className("BuildConfig")

    buildConfigField("APP_VERSION", project.version.toString())
}

tasks {
    val depsFile = layout.buildDirectory.file("module.fun.txt")

    val createModuleVersionFile by registering {
        doLast {
            depsFile.get().asFile.printWriter().use { pw ->
                pw.appendLine(project.version.toString())
            }
        }
    }

    jar {
        dependsOn(createModuleVersionFile)
        from(depsFile)
    }
}
