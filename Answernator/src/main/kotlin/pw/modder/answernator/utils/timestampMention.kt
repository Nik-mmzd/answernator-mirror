package pw.modder.answernator.utils

import kotlinx.datetime.Instant

enum class TimestampFormat(val suffix: String) {
    SHORT_TIME("t"), LONG_TIME("T"),
    SHORT_DATE("d"), LONG_DATE("D"),
    SHORT_DATETIME("f"), LONG_DATETIME("F"),
    RELATIVE("R")
}

fun Instant.mention() = "<t:$epochSeconds>"
fun Instant.mention(format: TimestampFormat) = "<t:$epochSeconds:${format.suffix}>"