package pw.modder.answernator4

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.sentry.Sentry
import pw.modder.answernator.utils.Globals
import pw.modder.answernator4.interaction.interactionCommandService
import pw.modder.answernator.utils.extensions.bot.zombieWatchdog
import pw.modder.answernator4.interaction.InteractionCommandList

// TEMP MAIN
suspend fun main() {
    val sentryDSN: String? = System.getenv("ANSWERNATOR_SENTRY_DSN")
    if (sentryDSN != null)
        Sentry.init(sentryDSN)

    InteractionCommandList.load()

    val bot = Kord(Globals.config.token)

    with(bot) {
        interactionCommandService()
        zombieWatchdog()
    }

    bot.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
