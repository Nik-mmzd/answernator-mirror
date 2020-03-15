package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.db.Db

@UnstableDefault
@DiskordDsl
fun Bot.greetingService() {
    userJoinedGuild {
        val config = Db.guilds.get(it.guildId)
        if (config.greetNewUsers && config.greetingsChannel.isNotEmpty()) {
            clientStore.channels[config.greetingsChannel].sendMessage(
                String.format(
                    config.greetingText,
                    it.user?.mention ?: "??!?? O_o",
                    clientStore.guilds[it.guildId].get().name
                ))
        }
    }
}