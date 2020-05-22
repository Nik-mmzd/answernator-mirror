package pw.modder.answernator.tools.commandTypes

import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*

interface LocalizedGuildOnlyCommand: LocalizedCommand {
    override val channels: EnumSet<Command.ChannelTypes>
        get() = EnumSet.of(Command.ChannelTypes.GUILD)
}