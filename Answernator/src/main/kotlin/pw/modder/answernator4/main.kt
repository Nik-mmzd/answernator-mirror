package pw.modder.answernator4

import dev.kord.core.Kord
import io.sentry.Sentry
import pw.modder.answernator4.interaction.interactionCommandService
import pw.modder.answernator.utils.extensions.bot.zombieWatchdog
import pw.modder.answernator4.interaction.InteractionCommandList

// TEMP MAIN
suspend fun main() {
    if (Env.SENTRY_DSN != null)
        Sentry.init(Env.SENTRY_DSN)

    InteractionCommandList.load()

    val bot = Kord(Env.BOT_TOKEN)

    with(bot) {
        interactionCommandService()
        zombieWatchdog()
    }

    bot.login()
}
