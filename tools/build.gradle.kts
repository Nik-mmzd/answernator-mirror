plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.palantir.git-version")
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder.answernator"
version = gitVersion(mapOf ("prefix" to "tools@"))


dependencies {
    implementation(project(":Answernator"))
}

tasks {
    compileKotlin {
        kotlinOptions.freeCompilerArgs+="-Xopt-in=kotlin.RequiresOptIn"
    }
    compileTestKotlin {
        kotlinOptions.freeCompilerArgs+="-Xopt-in=kotlin.RequiresOptIn"
    }

    val createModuleVersionFile by creating {
        doLast {
            file("$buildDir/module.tools.txt").printWriter().use { pw ->
                pw.appendLine(project.version.toString())
            }
        }
    }

    jar {
        dependsOn(createModuleVersionFile)
        from("$buildDir/module.tools.txt")
    }
}