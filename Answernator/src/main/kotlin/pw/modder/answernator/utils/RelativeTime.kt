package pw.modder.answernator.utils

import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

data class RelativeTime(
    val years: Int,
    val months: Int,
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int
) {
    companion object {
        fun fromMillis(milliseconds: Long) = RelativeTime(
            years = (TimeUnit.MILLISECONDS.toDays(milliseconds) / 365).toInt(),
            months = ((TimeUnit.MILLISECONDS.toDays(milliseconds) / 30) % TimeUnit.MILLISECONDS.toDays(milliseconds) / 365).toInt(),
            days = (TimeUnit.MILLISECONDS.toDays(milliseconds) % TimeUnit.MILLISECONDS.toDays(milliseconds) / 30).toInt(),
            hours = (TimeUnit.MILLISECONDS.toHours(milliseconds) % TimeUnit.DAYS.toHours(1)).toInt(),
            minutes = (TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1)).toInt(),
            seconds = (TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1)).toInt()
        )

        fun between(a: OffsetDateTime, b: OffsetDateTime) = fromMillis(a.toInstant().toEpochMilli() - b.toInstant().toEpochMilli())
    }
}