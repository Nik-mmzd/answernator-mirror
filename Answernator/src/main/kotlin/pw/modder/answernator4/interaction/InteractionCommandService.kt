package pw.modder.answernator4.interaction

import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import pw.modder.answernator4.Env
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent
import dev.kord.core.on
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

private fun List<Command>.toDesiredSet(): Set<Pair<String, ApplicationCommandType>> =
    map { it.effectiveName to it.discordType }.toSet()

private suspend fun Kord.deleteStaleGlobalCommands(desired: Set<Pair<String, ApplicationCommandType>>) {
    getGlobalApplicationCommands().collect { existing ->
        val type = existing.type
        if (type !is ApplicationCommandType.Unknown && (existing.name to type) !in desired) {
            logger.info { "Deleting stale global command: ${existing.name} (${existing.type})" }
            existing.delete()
        }
    }
}

private suspend fun Kord.deleteStaleGuildCommands(guildId: Snowflake, desired: Set<Pair<String, ApplicationCommandType>>) {
    getGuildApplicationCommands(guildId).collect { existing ->
        val type = existing.type
        if (type !is ApplicationCommandType.Unknown && (existing.name to type) !in desired) {
            logger.info { "Deleting stale guild command in $guildId: ${existing.name} (${existing.type})" }
            existing.delete()
        }
    }
}

suspend fun Kord.interactionCommandService() {
    val chatInput = InteractionCommandList.commands.filterIsInstance<ChatInputCommand>().associateBy { it.effectiveName }
    val user = InteractionCommandList.commands.filterIsInstance<UserCommand>().associateBy { it.effectiveName }
    val message = InteractionCommandList.commands.filterIsInstance<MessageCommand>().associateBy { it.effectiveName }

    val globalDesired = InteractionCommandList.commands.filter { it.guildIds.isEmpty() }.toDesiredSet()
    deleteStaleGlobalCommands(globalDesired)

    InteractionCommandList.commands.flatMap { it.guildIds }.toSet().forEach { guildId ->
        val guildDesired = InteractionCommandList.commands.filter { guildId in it.guildIds }.toDesiredSet()
        deleteStaleGuildCommands(guildId, guildDesired)
    }

    val cache = CommandRegistryCache(Env.COMMAND_HASH_CACHE)
    cache.load()

    InteractionCommandList.commands.forEach { command ->
        val cacheKey = "${command.effectiveName}:${command.discordType.value}"
        val hash = try { command.specHash() } catch (e: Exception) {
            logger.warn(e) { "Failed to compute spec hash for '${command.name}', will re-register" }
            null
        }
        if (hash != null && cache.isUnchanged(cacheKey, hash)) {
            logger.debug { "Skipping unchanged command: ${command.name}" }
            return@forEach
        }
        try {
            command.register(this)
            if (hash != null) cache.update(cacheKey, hash)
            logger.info { "Registered interaction command: ${command.name}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to register interaction command: ${command.name}" }
        }
    }

    cache.save()

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
