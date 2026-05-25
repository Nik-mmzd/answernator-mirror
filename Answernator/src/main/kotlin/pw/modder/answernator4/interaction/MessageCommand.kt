package pw.modder.answernator4.interaction

import dev.kord.core.Kord
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent

abstract class MessageCommand : Command() {
    override suspend fun register(kord: Kord) {
        kord.registerMessage(this)
    }

    abstract suspend fun MessageCommandInteractionCreateEvent.execute()
}