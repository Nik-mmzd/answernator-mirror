package pw.modder.answernator.utils

import java.io.InputStream
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

object Utils {
    fun getResource(path: String): InputStream {
        return javaClass.classLoader.getResourceAsStream(path)
            ?: throw Exception("Resource $path not found")
    }

    fun loadDependenciesList(): List<Dependency> {
        return getResource("dependencies.txt").reader().readLines().map {
            Dependency.fromString(it)
        }
    }

    fun getReadableUptime(): String {
        val uptime = ManagementFactory.getRuntimeMXBean().uptime

        return String.format(
            "%d days %02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toDays(uptime),
            TimeUnit.MILLISECONDS.toHours(uptime) % TimeUnit.DAYS.toHours(1),
            TimeUnit.MILLISECONDS.toMinutes(uptime) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(uptime) % TimeUnit.MINUTES.toSeconds(1)
        )
    }
}