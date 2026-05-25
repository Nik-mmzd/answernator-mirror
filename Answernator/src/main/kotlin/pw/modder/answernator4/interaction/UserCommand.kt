package pw.modder.answernator4.interaction

import dev.kord.core.Kord
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent

abstract class UserCommand : Command() {
    override suspend fun register(kord: Kord) {
        kord.registerUser(this)
    }

    abstract suspend fun UserCommandInteractionCreateEvent.execute()
}