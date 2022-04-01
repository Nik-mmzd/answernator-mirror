plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.palantir.git-version")
    id("com.github.johnrengelman.shadow")
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("Answernator") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder"
version = gitVersion()

val slf4jVersion: String by project
dependencies {
    runtimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "pw.modder.answernator.MainKt"
    }
}

tasks {
    val createDependenciesFile by creating {
        doLast {
            file("$buildDir/dependencies.txt").printWriter().use { pw ->
                pw.appendLine("${project.group}:${project.name}:${project.version}")
                configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts.forEach {
                    pw.appendLine(it.moduleVersion.toString())
                }
            }
        }
    }

    jar {
        dependsOn(createDependenciesFile)
        from("$buildDir/dependencies.txt")
    }

    shadowJar {
        dependsOn(createDependenciesFile)
        from("$buildDir/dependencies.txt")
    }
}