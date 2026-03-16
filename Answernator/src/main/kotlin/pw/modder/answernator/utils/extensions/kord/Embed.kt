package pw.modder.answernator.utils.extensions.kord

import dev.kord.rest.builder.message.EmbedBuilder
import kotlin.time.Clock

fun EmbedBuilder.timestampNow() {
    timestamp = Clock.System.now()
}