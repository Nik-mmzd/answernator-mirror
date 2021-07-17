package pw.modder.answernator.utils.extensions.bot

import dev.kord.common.entity.Permission
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import mu.KotlinLogging

import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.kord.channelType
import pw.modder.answernator.utils.extensions.kord.words
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

private val logger = KotlinLogging.logger {}

suspend fun Kord.commandService() {
    val config = Globals.config

    on<MessageCreateEvent> {
        if (message.content.isEmpty())
            return@on
        if (message.author == null)
            return@on
        if (message.author!!.isBot)
            return@on

        logger.debug { "received message, message text: ${message.content}" }
        message.data.guildId.value

        val guildConfig = message.data.guildId.value.takeUnless { it == null }?.run { Db.getGuildConfig(asString) }

        if (message.content.first() != guildConfig?.cmdPrefix ?: config.prefix)
            return@on

        val texts = LocaleBundle("botGlobal", guildConfig?.lang ?: config.lang)

        val words = message.words
        val command = CommandList.findCommand(name = words.first().drop(1), channelType = message.channelType)
            ?: return@on // if command not found: do nothing

        if (guildConfig != null && Db.isBlackListed(guildConfig.guildId, command.name)) {
            if (message.author?.id != message.getGuild().owner.id
                && message.getAuthorAsMember()?.getPermissions()?.contains(Permission.Administrator) != true)
                    return@on
        }

        logger.debug { "found command ${command.name}, checking" }
        if (!command.check(message, texts.locale)) {
            message.reply {
                content = texts.getString("bot.noPerms")
            }
            return@on
        }

        logger.debug { "found command ${command.name}, running" }
        try {
            command.action(message, words.drop(1), texts.locale)

        } catch (e: Exception) { // and any other exception
            logger.error(e) { "got error while running command" }
            message.reply { content = texts.formatString("bot.error", "${config.prefix}${command.name}") }
        }
    }
}