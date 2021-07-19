package pw.modder.answernator.utils.extensions.bot

import dev.kord.common.entity.Permission
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import mu.KotlinLogging
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.kord.*
import pw.modder.answernator.utils.locale.LocaleBundle

private val logger = KotlinLogging.logger {}

suspend fun Kord.commandService() {
    val config = Globals.config

    on<MessageCreateEvent> {
        if (message.content.isEmpty())
            return@on
        if (message.author?.isBot != false)
            return@on

        logger.debug { "received message, message text: ${message.content}" }

        val guildConfig = message.data.guildId.asOptional.orElse(null)
            ?.let { Db.getConfig(it) ?: Db.createDefaultConfig(it) }

        if (message.content.first() != guildConfig?.cmdPrefix ?: config.prefix)
            return@on

        val texts = LocaleBundle("botGlobal", guildConfig?.lang ?: config.lang)

        val words = message.words
        val command = CommandList.findCommand(name = words.first().drop(1), channelType = message.channelType)
            ?: return@on // if command not found: do nothing
        // command exists, start typing
        message.channel.withTyping {
            if (guildConfig != null && Db.isBlackListed(guildConfig.guildId, command.name)) {
                if (message.author?.id != message.getGuild().owner.id
                    && member?.getPermissions()?.contains(Permission.Administrator) != true)
                    return@withTyping
            }

            logger.debug { "found command ${command.name}, checking" }
            if (!command.check(message, texts.locale)) {
                message.reply(texts.getString("bot.noPerms"))
                return@withTyping
            }

            logger.debug { "found command ${command.name}, running" }

            try {
                command.action(message, words.drop(1).filter { it.isNotEmpty() }, texts.locale, guildConfig)

            } catch (e: Exception) { // and any other exception
                logger.error(e) { "got error while running command" }
                message.reply(texts.formatString("bot.error", "${config.prefix}${command.name}"))
            }
        }
    }
}