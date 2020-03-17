package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.UTF8Control
import pw.modder.answernator.utils.extensions.formatString
import pw.modder.answernator.utils.extensions.getStringOrKey
import java.util.*

private val logger = KotlinLogging.logger {}
@UnstableDefault
@DiskordDsl
fun Bot.commandService() {
    val config = Globals.config

    messageCreated { message: Message ->
        if (message.content.isEmpty()) return@messageCreated
        logger.debug { "received message, message text: ${message.content}" }
        if (message.content.first() != config.prefix) return@messageCreated

        val guildConfig = message.guildId?.run { Db.guilds.get(this) }
        val locale = guildConfig?.locale ?: config.locale
        val blacklist = guildConfig?.commandsBlacklist ?: listOf()
        val texts = ResourceBundle.getBundle("locale.botGlobal", locale, UTF8Control())

        val channelType =if (message.guildId == null) Command.ChannelTypes.DIRECT else Command.ChannelTypes.GUILD
        CommandList.commands.singleOrNull { command ->
            logger.debug { "probing command ${command.name}, searching for ${message.words.first()}" }
            message.words.first().equals("${config.prefix}${command.name}", true) && channelType in command.channels
        }?.run {
            logger.debug { "found command $name, running" }
            if (name in blacklist) {
                logger.debug { "command $name is blacklisted on this guild" }
                message.reply(texts.formatString("bot.blacklisted", name))
                return@messageCreated
            }
            if (check(message, message.guildId?.run { clientStore.guilds[this] })) {
                val reply = try {
                    action(this@commandService, message, locale)
                } catch (e: NotImplementedError) {
                    val text = e.message?.run { texts.formatString("bot.notImplemented.message", "${config.prefix}$name", this) }
                        ?: texts.formatString("bot.notImplemented", "${config.prefix}$name")
                    message.reply(text)
                    return@run
                } catch (e: Exception) {
                    logger.error(e) { "got error while running command" }
                    message.reply(
                        texts.formatString("bot.error", "${config.prefix}$name")
                    )
                    return@run
                }

                message.reply(reply.text, reply.embed())
                return@run
            }

            message.reply(
                texts.getStringOrKey("bot.noPerms")
            )
        }
    }
}