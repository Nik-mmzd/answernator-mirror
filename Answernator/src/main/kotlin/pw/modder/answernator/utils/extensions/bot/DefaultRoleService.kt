package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.db.Db

@UnstableDefault
@DiskordDsl
fun Bot.defaultRoleService() {
    userJoinedGuild {
        val config = Db.guilds.get(it.guildId)
        val guild = clientStore.guilds[it.guildId]
        val id = it.user?.id ?: return@userJoinedGuild
        config.defaultRole.takeIf { it.isNotEmpty() }?.run {
            guild.addMemberRole(userId = id, roleId = this)
        }
    }
}