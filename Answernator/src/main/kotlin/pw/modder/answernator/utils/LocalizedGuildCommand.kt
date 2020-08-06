package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import java.util.*

interface LocalizedGuildCommand: LocalizedCommand {
    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        when(val gid = message.guildId) {
            null -> throw Exception("No guild found but command is guild only")
            else -> return action(bot, message, texts, gid)
        }
    }

    override val channels: EnumSet<Command.ChannelTypes>
        get() = EnumSet.of(Command.ChannelTypes.GUILD)

    suspend fun action(bot: Bot, message: Message, texts: ResourceBundle, guildId: String): CombinedMessageEmbed
}