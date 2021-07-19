package pw.modder.answernator.utils.extensions.bot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.on
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features

suspend fun Kord.defaultRoleService() {
    on<MemberJoinEvent> {
        val config = Db.getGuildConfig(guildId) ?: return@on
        if (!config.isEnabled(Features.DEFAULT_ROLE)) return@on

        member.addRole(Snowflake(config.defaultRole ?: return@on), "Adding default role")
    }
}