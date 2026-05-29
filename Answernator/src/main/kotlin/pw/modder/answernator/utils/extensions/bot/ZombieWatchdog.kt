package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.gateway.ResumedEvent
import dev.kord.core.on
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

/** How long a zombie connection is tolerated before the process exits for the supervisor to restart. */
private val RESTART_DELAY = 30.seconds

/** Non-zero so a supervisor (systemd `Restart=on-failure`, docker restart policy) actually restarts us. */
private const val ZOMBIE_EXIT_CODE = 1

fun Kord.zombieWatchdog() {
    // Holds the single pending restart timer, if any. Cancelled when the connection is restored.
    val countdown = AtomicReference<Job?>(null)

    fun cancelCountdown(reason: String) {
        countdown.getAndSet(null)?.let {
            it.cancel()
            logger.info { reason }
        }
    }

    on<DisconnectEvent.ZombieConnectionEvent> {
        logger.warn { "Bot entered zombie connection state! Starting ${RESTART_DELAY.inWholeSeconds}-second restart countdown..." }
        val timer = launch(Dispatchers.Default) {
            delay(RESTART_DELAY)
            logger.error { "Zombie connection persists, stopping bot!" }
            exitProcess(ZOMBIE_EXIT_CODE)
        }
        // Replace any previous timer with this one and cancel the stale one, so overlapping
        // zombie events never spawn parallel countdowns.
        countdown.getAndSet(timer)?.cancel()
    }

    on<ReadyEvent> {
        cancelCountdown("Connection was restored, disabling restart countdown")
    }

    on<ResumedEvent> {
        cancelCountdown("Connection was restored, disabling restart countdown")
    }
}
