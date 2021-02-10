package pw.modder.answernator.utils

import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatterBuilder
import pw.modder.answernator.utils.locale.LocaleBundle
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

    private val String.s get() = " $this"

    fun prettyPrintPeriod(locale: Locale, time: Any, till: DateTime = DateTime.now()): String {
        val texts = LocaleBundle("botGlobal", locale)
        val formatter = PeriodFormatterBuilder()
            .appendYears()
            .appendSuffix(texts.getString("bot.date.year").s, texts.getString("bot.date.years").s)
            .appendSeparatorIfFieldsBefore(" ")
            .appendMonths()
            .appendSuffix(texts.getString("bot.date.month").s, texts.getString("bot.date.months").s)
            .appendSeparatorIfFieldsBefore(" ")
            .appendDays()
            .appendSuffix(texts.getString("bot.date.day").s, texts.getString("bot.date.days").s)
            .appendSeparatorIfFieldsBefore(" ")
            .appendHours()
            .appendSuffix(texts.getString("bot.date.hour").s, texts.getString("bot.date.hours").s)
            .appendSeparatorIfFieldsBefore(" ")
            .appendMinutes()
            .appendSuffix(texts.getString("bot.date.minute").s, texts.getString("bot.date.minutes").s)
            .appendSeparatorIfFieldsBefore(" ")
            .appendSeconds()
            .appendSuffix(texts.getString("bot.date.second").s, texts.getString("bot.date.seconds").s)
            .toFormatter()
        return formatter.print(Period(DateTime(time), till, PeriodType.yearMonthDayTime()))
    }

    fun prettyPrintPeriodSnowflake(locale: Locale, snowflake: String, till: DateTime = DateTime.now())
            = prettyPrintPeriod(locale, snowflakeCreatedAt(snowflake), till)

    fun snowflakeCreatedAt(snowflake: String): Long {
        return (snowflake.toLong() shr 22) + 1420070400000
    }
}