package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun CombinedMessageEmbed.setCurrentTimestamp() {
    timestamp = OffsetDateTime.now( ZoneOffset.UTC ).toString()
}