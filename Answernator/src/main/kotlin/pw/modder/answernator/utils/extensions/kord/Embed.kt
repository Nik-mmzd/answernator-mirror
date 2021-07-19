package pw.modder.answernator.utils.extensions.kord

import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.datetime.Clock

fun EmbedBuilder.timestampNow() {
    timestamp = Clock.System.now()
}