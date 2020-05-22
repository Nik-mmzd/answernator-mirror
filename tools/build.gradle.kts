plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.palantir.git-version")
}

val diskordVersion: String by project
val ktorVersion: String by project
val exposedVersion: String by project
val commonsIoVersion: String by project
val kotlinLoggingVersion: String by project
val guavaVersion: String by project
val jodaTimeVersion: String by project


val gitVersion: groovy.lang.Closure<String> by extra

group = "pw.modder.answernator"
version = gitVersion(mapOf ("prefix" to "tools@"))

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(project(":Answernator"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jessecorbett:diskord:$diskordVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("joda-time:joda-time:$jodaTimeVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
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