package pw.modder.answernator.utils

import kotlinx.io.IOException
import java.io.InputStream
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

object Utils {
    private fun getResource(path: String): InputStream {
        return javaClass.classLoader.getResourceAsStream(path)
            ?: throw IOException("Resource $path not found")
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
