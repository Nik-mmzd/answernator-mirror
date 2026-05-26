package pw.modder.answernator4.interaction

import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent
import dev.kord.core.on
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun Kord.interactionCommandService() {
    val chatInput = InteractionCommandList.commands.filterIsInstance<ChatInputCommand>().associateBy { it.name }
    val user = InteractionCommandList.commands.filterIsInstance<UserCommand>().associateBy { it.name }
    val message = InteractionCommandList.commands.filterIsInstance<MessageCommand>().associateBy { it.name }

    InteractionCommandList.commands.forEach { command ->
        try {
            command.register(this)
            logger.info { "Registered interaction command: ${command.name}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to register interaction command: ${command.name}" }
        }
    }

    on<ChatInputCommandInteractionCreateEvent> {
        val command = chatInput[interaction.command.rootName] ?: return@on
        try {
            with(command) { execute() }
        } catch (e: Exception) {
            logger.error(e) { "Error in chat input command '${command.name}'" }
        }
    }

    on<UserCommandInteractionCreateEvent> {
        val command = user[interaction.invokedCommandName] ?: return@on
        try {
            with(command) { execute() }
        } catch (e: Exception) {
            logger.error(e) { "Error in user command '${command.name}'" }
        }
    }

    on<MessageCommandInteractionCreateEvent> {
        val command = message[interaction.invokedCommandName] ?: return@on
        try {
            with(command) { execute() }
        } catch (e: Exception) {
            logger.error(e) { "Error in message command '${command.name}'" }
        }
    }
}
