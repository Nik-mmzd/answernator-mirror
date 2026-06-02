package pw.modder.answernator4

import dev.kord.core.Kord
import dev.kord.gateway.Intents
import io.sentry.Sentry
import org.kodein.di.direct
import org.kodein.di.instance
import pw.modder.answernator4.di.KodeinModuleList
import pw.modder.answernator4.di.buildDi
import pw.modder.answernator4.di.deployTo


suspend fun main() {
    if (Env.SENTRY_DSN != null)
        Sentry.init(Env.SENTRY_DSN)

    KodeinModuleList.load()

    val bot = Kord(Env.BOT_TOKEN)
    val di = buildDi(bot)

    di.deployTo(bot)

    val intentsList by di.instance<Set<Intents>>()
    bot.login {
        intentsList.forEach { intents += it }
    }
}
