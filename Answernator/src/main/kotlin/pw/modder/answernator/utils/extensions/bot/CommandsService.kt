package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.api.exception.DiscordBadPermissionsException
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.channelType
import pw.modder.answernator.utils.extensions.computePermissions
import pw.modder.answernator.utils.locale.LocaleBundle

private fun String.asMessage() = CombinedMessageEmbed(text = this)

private val logger = KotlinLogging.logger {}
@DiskordDsl
fun Bot.commandService() {
    val config = Globals.config

    messageCreated { message: Message ->
        if (message.content.isEmpty())
            return@messageCreated
        if (message.author.isBot)
            return@messageCreated

        logger.debug { "received message, message text: ${message.content}" }
        val guildConfig = message.guildId.takeUnless { it == null }?.run { Db.getGuildConfig(this) }

        if (message.content.first() != guildConfig?.cmdPrefix ?: config.prefix)
            return@messageCreated

        val texts = LocaleBundle("botGlobal", guildConfig?.lang ?: config.lang)

        val command = CommandList.findCommand(name = message.words.first().drop(1), channelType = message.channelType)
            ?: return@messageCreated // if command not found: do nothing

        val blacklisted = transaction { guildConfig?.blacklistedCommands }?.any { it.command.equals(command.name, true) } == true
        if (blacklisted && guildConfig != null) {
            val guild = clientStore.guilds[guildConfig.guildId].getCached()

            // if not author and not admin => blacklist
            if (message.authorId != guild.ownerId &&
                message.partialMember?.computePermissions(guild, message.authorId)?.contains(Permission.ADMINISTRATOR) != true)
                    return@messageCreated
        }

        logger.debug { "found command ${command.name}, checking" }
        if (!command.check(message, clientStore.guilds)) {
            message.reply(texts.getString("bot.noPerms"))
            return@messageCreated
        }

        logger.debug { "found command ${command.name}, running" }
        val reply = try {
            command.action(this, message, texts.locale)

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