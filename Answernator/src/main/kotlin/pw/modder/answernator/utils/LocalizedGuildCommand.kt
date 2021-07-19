package pw.modder.answernator.utils

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

interface LocalizedGuildCommand: LocalizedCommand {
    override val channels: EnumSet<Command.ChannelTypes>
        get() = EnumSet.of(Command.ChannelTypes.GUILD)

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle, config: Config?) {
        if (config == null) throw Exception("Cannot run guild command without config!")
        when (val guild = message.getGuildOrNull()) {
            null -> throw Exception("Guild not found, but command is guild only")
            else -> action(message, args, guild, texts, config)
        }
    }

    suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config)
}