package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import java.util.*

interface GuildCommand: Command {
    override val channels: EnumSet<Command.ChannelTypes>
        get() = EnumSet.of(Command.ChannelTypes.GUILD)

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        when (val gid = message.guildId) {
            null -> throw Exception("Guild not found, but command is guild only")
            else -> return action(bot, message, locale, gid)
        }
    }

    suspend fun action(bot: Bot, message: Message, locale: Locale, guildId: String): CombinedMessageEmbed
}