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
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs+="-Xopt-in=kotlin.RequiresOptIn"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs+="-Xopt-in=kotlin.RequiresOptIn"
    }

    val createModuleVersionFile by creating {
        doLast {
            file("$buildDir/module.tools.txt").printWriter().use { pw ->
                pw.append(project.version.toString())
                pw.appendln()
            }
        }
    }

    jar {
        dependsOn(createModuleVersionFile)
        from("$buildDir/module.tools.txt")
    }
}