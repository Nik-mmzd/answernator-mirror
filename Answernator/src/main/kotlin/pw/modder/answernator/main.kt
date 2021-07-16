package pw.modder.answernator

import dev.kord.core.Kord
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.bot.*

suspend fun main() {
    Db.initDb()
    CommandList.load()

    val bot = Kord(Globals.config.token)

    with(bot) {
        commandService()
        greetingService()
        defaultStatusService()
        muteService()
        defaultRoleService()
        logService()
        antiSpam()
    }

    bot.login()
}
