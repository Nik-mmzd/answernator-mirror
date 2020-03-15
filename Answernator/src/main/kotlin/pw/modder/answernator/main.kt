package pw.modder.answernator

import com.jessecorbett.diskord.dsl.bot
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.cache.GuildCache.enableGuildCache
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.bot.*

@UnstableDefault
suspend fun main() {
    CommandList.load()

    bot(Globals.config.token) {
        enableGuildCache()
        commandService()
        greetingService()
        defaultStatusService()
        muteService()
        defaultRoleService()
        logService()
    }
}
