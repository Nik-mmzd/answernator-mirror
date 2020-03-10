package pw.modder.answernator

import com.jessecorbett.diskord.dsl.bot
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.cache.GuildCache.enableGuildCache
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.defaultStatusService
import pw.modder.answernator.utils.extensions.greetingsService
import pw.modder.answernator.utils.extensions.loadCommandService

@UnstableDefault
suspend fun main() {
    CommandList.load()

    bot(Globals.config.token) {
        enableGuildCache()
        loadCommandService()
        greetingsService()
        defaultStatusService()
    }
}
