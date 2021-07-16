package pw.modder.answernator.utils.extensions.bot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.on
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features

suspend fun Kord.greetingService() {
    on<MemberJoinEvent> {
        this.member.mention
        val config = Db.getGuildConfig(guildId.asString)
        if (config.isEnabled(Features.GREETING) && config.greetingChannel != null) {
            rest.channel.createMessage(Snowflake(config.greetingChannel!!)) {
                content = config.greeting.format(member.mention, guild.asGuild().name)
            }
        }
    }
}