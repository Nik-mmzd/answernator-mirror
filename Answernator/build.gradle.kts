plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.palantir.git-version")
    id("com.github.johnrengelman.shadow")
}

val diskordVersion: String by project
val exposedVersion: String by project
val ktorVersion: String by project


val gitVersion: groovy.lang.Closure<*> by extra

group = "pw.modder"
version = gitVersion.call()

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jessecorbett:diskord-jvm:$diskordVersion")
    implementation("org.slf4j:slf4j-simple:1.7.26")
    implementation("commons-io:commons-io:2.6")
    implementation("io.github.microutils:kotlin-logging:1.7.8")
    implementation("com.google.guava:guava:28.2-jre")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.h2database:h2:1.4.200")
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