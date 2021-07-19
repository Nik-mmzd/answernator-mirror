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

val gitVersion: groovy.lang.Closure<*> by extra

group = "pw.modder"
version = gitVersion.call()

val slf4jVersion: String by project
dependencies {
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
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
                pw.append("${project.group}:${project.name}:${project.version}")
                pw.appendln()
                configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts.forEach {
                    pw.append(it.moduleVersion.toString())
                    pw.appendln()
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