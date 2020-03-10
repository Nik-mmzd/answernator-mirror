package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun CombinedMessageEmbed.setCurrentTimestamp() {
    timestamp = OffsetDateTime.now( ZoneOffset.UTC ).toString()
}

fun CombinedMessageEmbed.setTimestamp(ts: Long) {
    timestamp = OffsetDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneOffset.UTC).toString()
}