package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.on
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features

fun Kord.greetingService() {
    on<MemberJoinEvent> {
        val config = Db.getGuildConfig(guildId.asString)
        if (config.isEnabled(Features.GREETING) && config.greetingChannel != null) {
            TODO()
        }
    }
    userJoinedGuild {
        val config = Db.getGuildConfig(it.guildId)
        if (config.isEnabled(Features.GREETING) && config.greetingChannel != null) {
            clientStore.channels[config.greetingChannel!!].sendMessage(
                String.format(
                    config.greeting,
                    it.user?.mention ?: "??!?? O_o",
                    clientStore.guilds[it.guildId].get().name
                ))
        }
    }
}