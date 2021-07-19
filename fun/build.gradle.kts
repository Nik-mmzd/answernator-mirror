plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.palantir.git-version")
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder.answernator"
version = gitVersion(mapOf ("prefix" to "fun@"))

dependencies {
    implementation(project(":Answernator"))
}

tasks {
    val createModuleVersionFile by creating {
        doLast {
            file("$buildDir/module.fun.txt").printWriter().use { pw ->
                pw.append(project.version.toString())
                pw.appendLine()
            }
        }
    }

    jar {
        dependsOn(createModuleVersionFile)
        from("$buildDir/module.fun.txt")
    }
}