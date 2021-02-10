package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.api.exception.DiscordBadPermissionsException
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.words
import mu.KotlinLogging
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.GuildConfig
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.channelType
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

private fun String.asMessage() = CombinedMessageEmbed(text = this)

private fun Message.getGuildConfig(): GuildConfig? {
    return Db.guilds.get(guildId ?: return null)
}

private val logger = KotlinLogging.logger {}
@DiskordDsl
fun Bot.commandService() {
    val config = Globals.config

    messageCreated { message: Message ->
        if (message.content.isEmpty()) return@messageCreated
        logger.debug { "received message, message text: ${message.content}" }
        if (message.content.first() != config.prefix) return@messageCreated

        val guildConfig = message.getGuildConfig()
        val locale = Locale(guildConfig?.lang ?: config.lang)
//        val blacklist = guildConfig?.run { commandsBlacklist.split('|') } ?: listOf()
        val texts = LocaleBundle("botGlobal", locale)

        val command = CommandList.findCommand(name = message.words.first().drop(1), channelType = message.channelType)
            ?: return@messageCreated // if command not found: do nothing

        logger.debug { "found command ${command.name}, checking" }
        if (!command.check(message, clientStore.guilds)) {
            message.reply(texts.getString("bot.noPerms"))
            return@messageCreated
        }

        logger.debug { "found command ${command.name}, running" }
        val reply = try {
            command.action(this, message, locale)

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