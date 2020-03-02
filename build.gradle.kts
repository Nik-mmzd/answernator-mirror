plugins {
    kotlin("jvm") version "1.3.61"
    id("kotlinx-serialization") version "1.3.50"
    id("com.palantir.git-version") version "0.12.2"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

val gitVersion: groovy.lang.Closure<*> by extra

group = "pw.modder"
version = gitVersion.call()

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jessecorbett:diskord-jvm:1.5.3")
    implementation("org.slf4j:slf4j-simple:1.7.26")
    implementation("commons-io:commons-io:2.6")
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "pw.modder.answernator.MainKt"
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

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