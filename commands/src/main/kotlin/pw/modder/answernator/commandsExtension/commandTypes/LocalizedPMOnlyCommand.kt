package pw.modder.answernator.commandsExtension.commandTypes

import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*

@UnstableDefault
interface LocalizedPMOnlyCommand: LocalizedCommand {
    override val channels: EnumSet<Command.ChannelTypes>
        get() = EnumSet.of(Command.ChannelTypes.DIRECT)
}