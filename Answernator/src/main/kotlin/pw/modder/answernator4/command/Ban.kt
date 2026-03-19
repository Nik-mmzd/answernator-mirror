package pw.modder.answernator4.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.InteractionCreateEvent
import pw.modder.answernator4.interaction.*

object Ban : Command() {
    override val name = "ban"
    override val bundleName = "ban"
    override val description = "command.ban.description".asLocaleKey()

    val target: Option<Snowflake> by userId()
    val reason: Option<String?> by string().optional()

    override suspend fun register(kord: Kord) {
        kord.register(this)
    }

    override suspend fun InteractionCreateEvent.execute() {
        val target: Snowflake by option(target)
        val reason: String? by option(reason)
        
        val banMessage = bundle.getString("ban.success") ?: "Banned"
        // Implementation of ban logic would go here
    }
}