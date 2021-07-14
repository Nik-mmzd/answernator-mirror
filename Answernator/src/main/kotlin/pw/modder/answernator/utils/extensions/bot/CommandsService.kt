package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
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

fun Kord.commandService() {
    val config = Globals.config

    on<MessageCreateEvent> {
        if (message.content.isEmpty())
            return@on
        if (message.author?.isBot == true)
            return@on

        logger.debug { "received message, message text: ${message.content}" }
        message.data.guildId.value

        val guildConfig = message.data.guildId.value.takeUnless { it == null }?.run { Db.getGuildConfig(asString) }

        if (message.content.first() != guildConfig?.cmdPrefix ?: config.prefix)
            return@on

        val texts = LocaleBundle("botGlobal", guildConfig?.lang ?: config.lang)

        val command = CommandList.findCommand(name = message.words.first().drop(1), channelType = message.channelType)
            ?: return@messageCreated // if command not found: do nothing

        if (guildConfig != null && Db.isBlackListed(guildConfig.guildId, command.name)) {
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