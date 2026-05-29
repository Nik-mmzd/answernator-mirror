package pw.modder.answernator4.interaction

import dev.kord.common.entity.ApplicationCommandType
import dev.kord.core.Kord
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import org.kodein.di.DI

abstract class MessageCommand(di: DI) : Command(di) {
    override val discordType = ApplicationCommandType.Message
    override suspend fun register(kord: Kord) {
        kord.registerMessage(this)
    }

    abstract suspend fun MessageCommandInteractionCreateEvent.execute()
}
