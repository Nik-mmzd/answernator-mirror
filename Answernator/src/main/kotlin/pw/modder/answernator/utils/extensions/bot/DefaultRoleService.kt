package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features

@DiskordDsl
fun Bot.defaultRoleService() {
    userJoinedGuild {
        val config = Db.getGuildConfig(it.guildId)
        if (!config.isEnabled(Features.DEFAULT_ROLE)) return@userJoinedGuild
        val guild = clientStore.guilds[it.guildId]
        val id = it.user?.id ?: return@userJoinedGuild
        config.defaultRole?.run {
            guild.addMemberRole(userId = id, roleId = this)
        }
    }
}