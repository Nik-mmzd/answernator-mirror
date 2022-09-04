val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder.answernator"
version = gitVersion(mapOf ("prefix" to "tools@"))


dependencies {
    implementation(project(":Answernator"))
    implementation(libs.apache.commons.io)
}

tasks {
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