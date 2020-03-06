plugins {
    kotlin("jvm")
    id("kotlinx-serialization")
    id("com.palantir.git-version")
    id("com.github.johnrengelman.shadow")
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
    implementation("io.github.microutils:kotlin-logging:1.7.8")
    implementation("com.google.guava:guava:28.2-jre")
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