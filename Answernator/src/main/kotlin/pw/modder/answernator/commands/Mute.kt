package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.GuildClients
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import mu.KotlinLogging
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.Db.memberIsMuted
import pw.modder.answernator.db.Db.muteMember
import pw.modder.answernator.db.Db.unmuteMember
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.bot.isMe
import pw.modder.answernator.utils.extensions.isAdmin
import pw.modder.answernator.utils.extensions.isUserMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

private val logger = KotlinLogging.logger {  }
class Mute: LocalizedGuildCommand {
    override val name = "mute"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_MESSAGES
    override val cmdType = Command.CommandGroup.MODER
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val requiredPermission: Permission? = Permission.MANAGE_ROLES

    override fun check(message: Message, permissions: Permissions): Boolean {
        return super.check(message, permissions) && Db.guilds.get(message.guildId ?: return false).muteRole.isNotEmpty()
    }

    override suspend fun check(message: Message, guildClients: GuildClients): Boolean {
        return super.check(message, guildClients) && Db.guilds.get(message.guildId ?: return false).muteRole.isNotEmpty()
    }

    override suspend fun action(
        bot: Bot,
        message: Message,
        texts: CommandLocaleBundle,
        guildId: String
    ): CombinedMessageEmbed {
        logger.debug { "Getting guild..." }
        val guildClient = bot.clientStore.guilds[guildId]
        val guild = guildClient.getCached()
        val muteRole = Db.guilds.get(guild.id).muteRole

        logger.debug { "Checking configs" }
        if (muteRole.isEmpty()) return texts.getString("not.configured").toMessage()
        if (message.words.size < 2) return texts.getErrorString().toMessage()
        if (!message.words[1].isUserMention()) return texts.getErrorString().toMessage()

        if (message.usersMentioned.size != 1) {
            return texts.getErrorString().toMessage()
        }

        with(message.usersMentioned.single()) {
            if (bot.isMe(id)) return texts.formatString("muted.self", mention).toMessage() // easter egg
            val member = guildClient.getMember(id)
            if (member.isAdmin(guild, id)) return texts.formatString("error.whitelisted", mention).toMessage()
            logger.debug { "Got mentioned user" }

            if (guild.memberIsMuted(id)) {
                logger.debug { "Member is muted" }
                return texts.formatString("log.muted.already", mention).toMessage()
            }

            logger.debug { "Member is not muted" }
            val reason = message.words.drop(2).joinToString(" ").ifEmpty {
                texts.getRandomString("reason")
            }
            logger.debug { "Got mute reason" }

            with(Db.logs.get(guild.id)) {
                if (memberUnmuteLogChannel.isNotEmpty()) try {
                    bot.clientStore.channels[memberUnmuteLogChannel]
                        .sendMessage(texts.formatString("log.muted", mention, message.author.mention, reason))
                    logger.debug { "LOG message sent" }
                } catch (e: Exception) {
                    logger.warn(e) { "${guild.name} (${guild.id}): Log error" }
                }
            }

            guild.muteMember(id)
            logger.debug { "Member muted in DB" }

            try {
                guildClient.addMemberRole(userId = id, roleId = muteRole)
                logger.debug { "Added mute role" }
            } catch (e: Exception) {
                logger.error(e) { "Error adding mute role for user $username at ${guild.name}, role id $muteRole. Member is UNMUTED in DB" }
                guild.unmuteMember(id)
                throw e
            }

            return texts.formatString("muted", mention, reason).toMessage()
        }
    }
}

class Unmute: LocalizedGuildCommand {
    override val name = "unmute"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_MESSAGES
    override val cmdType = Command.CommandGroup.MODER
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val requiredPermission: Permission? = Permission.MANAGE_ROLES

    override fun check(message: Message, permissions: Permissions): Boolean {
        return super.check(message, permissions) && Db.guilds.get(message.guildId ?: return false).muteRole.isNotEmpty()
    }

    override suspend fun check(message: Message, guildClients: GuildClients): Boolean {
        return super.check(message, guildClients) && Db.guilds.get(message.guildId ?: return false).muteRole.isNotEmpty()
    }

    override fun getTexts(locale: Locale): CommandLocaleBundle {
        return CommandLocaleBundle("mute", locale, javaClass.classLoader)
    }

    override suspend fun action(
        bot: Bot,
        message: Message,
        texts: CommandLocaleBundle,
        guildId: String
    ): CombinedMessageEmbed {
        logger.debug { "Getting guild..." }
        val guildClient = bot.clientStore.guilds[guildId]
        val guild = guildClient.getCached()
        val muteRole = Db.guilds.get(guild.id).muteRole

        logger.debug { "Checking configs" }
        if (muteRole.isEmpty()) return texts.getString("not.configured").toMessage()
        if (message.words.size < 2) return texts.getErrorString().toMessage()
        if (!message.words[1].isUserMention()) return texts.getErrorString().toMessage()

        if (message.usersMentioned.size != 1) {
            return texts.getErrorString().toMessage()
        }

        with(message.usersMentioned.single()) {
            val member = guildClient.getMember(id)
            if (member.isAdmin(guild, id) || bot.isMe(id)) return texts.formatString("error.whitelisted", mention).toMessage()
            logger.debug { "Got mentioned user" }

            if (!guild.memberIsMuted(id)) {
                logger.debug { "Member is not muted" }
                return texts.formatString("log.unmuted.already", mention).toMessage()
            }

            logger.debug { "Member is muted" }
            guild.unmuteMember(id)
            logger.debug { "Unmuted user in DB" }

            with(Db.logs.get(guild.id)) {
                if (memberUnmuteLogChannel.isNotEmpty()) try {
                    bot.clientStore.channels[memberUnmuteLogChannel]
                        .sendMessage(texts.formatString("log.unmuted", mention, message.author.mention))
                    logger.debug { "LOG message sent" }
                } catch (e: Exception) {
                    logger.warn(e) { "${guild.name} (${guild.id}): Log error" }
                }
            }

            try {
                guildClient.removeMemberRole(userId = id, roleId = muteRole)
                logger.debug { "Removed mute role" }
            } catch (e: Exception) {
                logger.error(e) { "Error removing mute role for user $username at ${guild.name}, role id $muteRole. Member is UNMUTED in DB" }
                throw e
            }

            return texts.formatString("unmuted", mention).toMessage()
        }
    }
}