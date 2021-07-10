package pw.modder.answernator

import com.jessecorbett.diskord.dsl.bot
import pw.modder.answernator.cache.GuildCache.enableGuildCache
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.bot.*

suspend fun main() {
    Db.initDb()
    CommandList.load()

    bot(Globals.config.token) {
        enableGuildCache()
        commandService()
        greetingService()
        defaultStatusService()
        muteService()
        defaultRoleService()
        logService()
        antiSpam()
    }
}
