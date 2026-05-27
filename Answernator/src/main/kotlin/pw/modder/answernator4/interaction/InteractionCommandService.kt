package pw.modder.answernator4.interaction

import dev.kord.core.Kord
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent
import dev.kord.core.on
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun Kord.interactionCommandService() {
    val chatInput = InteractionCommandList.commands.filterIsInstance<ChatInputCommand>().associateBy { it.effectiveName }
    val user = InteractionCommandList.commands.filterIsInstance<UserCommand>().associateBy { it.effectiveName }
    val message = InteractionCommandList.commands.filterIsInstance<MessageCommand>().associateBy { it.effectiveName }

    InteractionCommandList.commands.forEach { command ->
        try {
            command.register(this)
            logger.info { "Registered interaction command: ${command.name}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to register interaction command: ${command.name}" }
        }
    }

    on<InteractionCreateEvent> {
        logger.debug { "Event ${this::class.simpleName} Interaction ${interaction::class.simpleName} created: $interaction" }
    }

    on<ChatInputCommandInteractionCreateEvent> {
        logger.debug { "Got ChatInputCommandInteractionCreateEvent with command ${interaction.invokedCommandName} (${interaction.command.rootName})" }
        val command = chatInput[interaction.command.rootName] ?: return@on
        try {
            with(command) { execute() }
        } catch (e: Exception) {
            logger.error(e) { "Error in chat input command '${command.name}'" }
        }
    }

    on<UserCommandInteractionCreateEvent> {
        logger.debug { "Got UserCommandInteractionCreateEvent with command ${interaction.invokedCommandName}" }
        val command = user[interaction.invokedCommandName] ?: return@on
        try {
            with(command) { execute() }
        } catch (e: Exception) {
            logger.error(e) { "Error in user command '${command.name}'" }
        }
    }

    on<MessageCommandInteractionCreateEvent> {
        logger.debug { "Got MessageCommandInteractionCreateEvent with command ${interaction.invokedCommandName}" }
        val command = message[interaction.invokedCommandName] ?: return@on
        try {
            with(command) { execute() }
        } catch (e: Exception) {
            logger.error(e) { "Error in message command '${command.name}'" }
        }
    }

    on<ButtonInteractionCreateEvent> {
        val parts = interaction.componentId.split(":", limit = 3)
        if (parts.size != 3 || parts[0] != "cmd") return@on
        val commandName = parts[1]
        val buttonId = parts[2]

        val command = chatInput[commandName] ?: user[commandName] ?: message[commandName] ?: return@on
        val button = command.buttons?.buttons?.firstOrNull { it.id == buttonId } ?: return@on

        try {
            with(command) { onButtonClick(button) }
        } catch (e: Exception) {
            logger.error(e) { "Error in command '$commandName' button click '$buttonId'" }
        }
    }
}
