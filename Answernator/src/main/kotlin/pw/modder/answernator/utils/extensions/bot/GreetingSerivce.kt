package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features

@DiskordDsl
fun Bot.greetingService() {
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