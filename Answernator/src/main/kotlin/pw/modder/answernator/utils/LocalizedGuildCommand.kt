package pw.modder.answernator.utils

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

interface LocalizedGuildCommand: LocalizedCommand {
    override val channels: EnumSet<Command.ChannelTypes>
        get() = EnumSet.of(Command.ChannelTypes.GUILD)

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle) {
        when (val guild = message.getGuildOrNull()) {
            null -> throw Exception("Guild not found, but command is guild only")
            else -> action(message, args, guild, texts)
        }
    }

    suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle)
}