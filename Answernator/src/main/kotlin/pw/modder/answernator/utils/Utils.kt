package pw.modder.answernator.utils

import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatterBuilder
import pw.modder.answernator.utils.extensions.getStringOrKey
import java.io.InputStream
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.TimeUnit

object Utils {
    private fun getResource(path: String): InputStream {
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

    fun prettyPrintTime(locale: Locale, millis: Long): String {
        val texts = ResourceBundle.getBundle("locale.botGlobal", locale, UTF8Control())
        val formatter = PeriodFormatterBuilder()
            .appendYears()
            .appendSuffix(texts.getStringOrKey("bot.date.year"), texts.getStringOrKey("bot.date.years"))
            .appendSeparatorIfFieldsBefore(" ")
            .appendMonths()
            .appendSuffix(texts.getStringOrKey("bot.date.month"), texts.getStringOrKey("bot.date.months"))
            .appendSeparatorIfFieldsBefore(" ")
            .appendDays()
            .appendSuffix(texts.getStringOrKey("bot.date.day"), texts.getStringOrKey("bot.date.days"))
            .appendSeparatorIfFieldsBefore(" ")
            .appendHours()
            .appendSuffix(texts.getStringOrKey("bot.date.hour"), texts.getStringOrKey("bot.date.hours"))
            .appendSeparatorIfFieldsBefore(" ")
            .appendMinutes()
            .appendSuffix(texts.getStringOrKey("bot.date.minute"), texts.getStringOrKey("bot.date.minutes"))
            .appendSeparatorIfFieldsBefore(" ")
            .appendSeconds()
            .appendSuffix(texts.getStringOrKey("bot.date.second"), texts.getStringOrKey("bot.date.seconds"))
            .toFormatter()
        return formatter.print(Duration(millis).toPeriod(PeriodType.yearMonthDayTime()))
    }
}