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
private val logger = KotlinLogging.logger {}

fun Kord.commandService() {
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

        val command = CommandList.findCommand(name = message.words.first().drop(1), channelType = message.channelType)
            ?: return@on // if command not found: do nothing

        if (guildConfig != null && Db.isBlackListed(guildConfig.guildId, command.name)) {
            if (message.author?.id != message.getGuild().owner.id
                && message.getAuthorAsMember()?.getPermissions()?.contains(Permission.Administrator) != true)
                    return@on
        }

        logger.debug { "found command ${command.name}, checking" }
        if (!command.check(message)) {
            message.reply {
                content = texts.getString("bot.noPerms")
            }
            return@on
        }

        logger.debug { "found command ${command.name}, running" }
        val reply = try {
            command.action(message)

        } catch (e: DiscordBadPermissionsException) { // Bot is missing permissions to run this command
            with(command.requiredPermission) {
                if (this == null) {
                    texts.getString("bot.badPermissions")
                } else {
                    texts.formatString(
                        "bot.badPermissions.perm",
                        texts.getNullableString("bot.badPermissions.${name}") ?: name
                    )
                }
            }.asMessage()

        } catch (e: NotImplementedError) { // if feature is not implemented
            with(e.message) {
                if (this == null) {
                    texts.formatString("bot.notImplemented", "${config.prefix}${command.name}")
                } else {
                    texts.formatString("bot.notImplemented.message", "${config.prefix}${command.name}", this)
                }
            }.asMessage()

        } catch (e: Exception) { // and any other exception
            logger.error(e) { "got error while running command" }
            texts.formatString("bot.error", "${config.prefix}${command.name}").asMessage()
        }

        message.reply(reply.text, reply.embed())
    }
}