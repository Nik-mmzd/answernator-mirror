package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun CombinedMessageEmbed.setCurrentTimestamp() {
    timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
}