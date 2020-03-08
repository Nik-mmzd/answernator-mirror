package pw.modder.answernator

import com.jessecorbett.diskord.dsl.bot
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.cache.GuildMemberRolesCache.enableGuildMemberRolesCache
import pw.modder.answernator.cache.GuildOwnerCache.enableGuildOwnerCache
import pw.modder.answernator.cache.RolesCache.enableRolesCache
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.GlobalConfig
import pw.modder.answernator.utils.extensions.loadCommandService

@UnstableDefault
suspend fun main() {
    CommandList.load()

    bot(GlobalConfig.get().token) {
        enableGuildMemberRolesCache()
        enableGuildOwnerCache()
        enableRolesCache()
        loadCommandService()
    }
}
