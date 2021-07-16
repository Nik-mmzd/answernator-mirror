package pw.modder.answernator.utils

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import java.util.*

interface GuildCommand: Command {
    override val channels: EnumSet<Command.ChannelTypes>
        get() = EnumSet.of(Command.ChannelTypes.GUILD)

    override suspend fun action(message: Message, args: List<String>, locale: Locale) {
        when (val guild = message.getGuildOrNull()) {
            null -> throw Exception("Guild not found, but command is guild only")
            else -> action(message, args, guild, locale)
        }
    }

    suspend fun action(message: Message, args: List<String>, guild: Guild, locale: Locale)
}