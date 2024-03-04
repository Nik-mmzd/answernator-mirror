package pw.modder.answernator

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.sentry.Sentry
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.bot.*

suspend fun main() {
    val sentryDSN: String? = System.getenv("ANSWERNATOR_SENTRY_DSN")
    if (sentryDSN != null)
        Sentry.init(sentryDSN)

    Db.initDb()
    CommandList.load()

    val bot = Kord(Globals.config.token)

    with(bot) {
        startDebug()
        configService()
        commandService()
        greetingService()
        defaultStatusService()
        muteService()
        defaultRoleService()
        logService()
        antiSpam()
    }

    bot.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
