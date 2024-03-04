package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.event.UnknownEvent
import dev.kord.core.event.gateway.GatewayEvent
import dev.kord.core.on
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {  }

fun Kord.startDebug() {
    on<GatewayEvent> {
        logger.debug { "Gateway Event: ${this::class.simpleName}" }
    }

    on<UnknownEvent> {
        logger.debug { "Unknown Gateway Event: ${name ?: "null"}" }
    }
}