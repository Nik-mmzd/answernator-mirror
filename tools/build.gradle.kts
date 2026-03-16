val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder.answernator"
version = gitVersion(mapOf ("prefix" to "tools@"))


dependencies {
    implementation(project(":Answernator"))
    implementation(libs.apache.commons.io)
}

tasks {
    val depsFile = layout.buildDirectory.file("module.tools.txt")

    val createModuleVersionFile by creating {
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