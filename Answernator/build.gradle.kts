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

val diskordVersion: String by project
val exposedVersion: String by project
val ktorVersion: String by project
val slf4jVersion: String by project
val commonsIoVersion: String by project
val kotlinLoggingVersion: String by project
val guavaVersion: String by project
val h2Version: String by project
val jodaTimeVersion: String by project


val gitVersion: groovy.lang.Closure<*> by extra

group = "pw.modder"
version = gitVersion.call()

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jessecorbett:diskord:$diskordVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.h2database:h2:$h2Version")
    implementation("joda-time:joda-time:$jodaTimeVersion")
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "pw.modder.answernator.MainKt"
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn" // for custom GuildClient.getAuditLog
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn" // for custom GuildClient.getAuditLog
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