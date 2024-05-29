package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.gateway.ResumedEvent
import dev.kord.core.on
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private val state = AtomicBoolean(false)
private val logger = KotlinLogging.logger {  }

fun Kord.zombieWatchdog() {
    on<DisconnectEvent.ZombieConnectionEvent> {
        logger.warn { "Bot entered zombie connection state! Starting 30-seconds restart countdown..." }
        state.set(true)
        launch(Dispatchers.Default) {
            delay(30.seconds)
            val state = state.get()
            logger.info { "Zombie connection state: $state" }
            if (state) {
                logger.error { "Zombie connection persists, stopping bot!" }
                exitProcess(0)
            }
        }
    }

    on<ReadyEvent> {
        val oldValue = state.getAndSet(false)
        if (oldValue) {
            logger.info { "Connection was restored, disabling restart countdown" }
        }
    }

    on<ResumedEvent> {
        val oldValue = state.getAndSet(false)
        if (oldValue) {
            logger.info { "Connection was restored, disabling restart countdown" }
        }
    }
}