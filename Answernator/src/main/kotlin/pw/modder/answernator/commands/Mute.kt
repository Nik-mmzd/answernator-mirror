package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
import pw.modder.answernator.db.Db.muteMember
import pw.modder.answernator.db.Db.unmuteMember
import pw.modder.answernator.db.Db.memberIsMuted
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.bot.isMe
import pw.modder.answernator.utils.extensions.isAdmin
import pw.modder.answernator.utils.extensions.isUserMention
import java.util.*

private val logger = KotlinLogging.logger {  }
@UnstableDefault
class Mute: LocalizedCommand {
    override val name = "mute"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_MESSAGES
    override val cmdType = Command.CommandGroup.MODER
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val requiredPermission: Permission? = Permission.MANAGE_ROLES

    override fun check(message: Message, permissions: Permissions): Boolean {
        return super.check(message, permissions) && Db.guilds.get(message.guildId ?: return false).muteRole.isNotEmpty()
    }

    override suspend fun check(message: Message, guildClient: GuildClient?): Boolean {
        return super.check(message, guildClient) && Db.guilds.get(message.guildId ?: return false).muteRole.isNotEmpty()
    }

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        logger.debug { "Getting guild..." }
        val guildClient = bot.clientStore.guilds[message.guildId ?: return texts.errorMessage()]
        val guild = guildClient.getCached()
        val muteRole = Db.guilds.get(guild.id).muteRole

        logger.debug { "Checking configs" }
        if (muteRole.isEmpty()) return textMessage(texts.getStringOrKey("not.configured"))
        if (message.words.size < 2) return texts.errorMessage()
        if (!message.words[1].isUserMention()) return texts.errorMessage()

        message.usersMentioned.singleOrNull()?.run {
            val member = guildClient.getMember(id)
            if (member.isAdmin(guild, id) || bot.isMe(id)) return texts.message("error.whitelisted", mention)
            logger.debug { "Got mentioned user" }
            val logconfig = Db.logs.get(guild.id)
            return when(guild.memberIsMuted(id)) {
                true -> {
                    logger.debug { "Member is muted" }

                    guild.unmuteMember(id)
                    logger.debug { "Unmuted user in DB" }

                    if (logconfig.memberUnmuteLogChannel.isNotEmpty()) try {
                        bot.clientStore.channels[logconfig.memberUnmuteLogChannel]
                            .sendMessage(texts.formatString("log.unmuted", mention, message.author.mention))
                        logger.debug { "LOG message sent" }
                    } catch (e: Exception) {
                        logger.warn(e) { "${guild.name} (${guild.id}): Log error" }
                    }

                    try {
                        guildClient.removeMemberRole(userId = id, roleId = muteRole)
                        logger.debug { "Removed mute role" }
                    } catch (e: Exception) {
                        logger.error(e) { "Error removing mute role for user $username at ${guild.name}, role id $muteRole. Member is UNMUTED in DB" }
                        throw e
                    }

                    textMessage(texts.formatString("unmuted", mention))
                }

                false -> {
                    logger.debug { "Member is not muted" }

                    val reason = message.words.drop(2).joinToString(" ").ifEmpty {
                        val reasonCount = texts.getStringOrKey("reason.count").toIntOrNull() ?: 1
                        texts.getStringOrKey("reason.${Globals.random.nextInt(0, reasonCount)}")

                    }
                    logger.debug { "Got mute reason" }

                    if (logconfig.memberUnmuteLogChannel.isNotEmpty()) try {
                        bot.clientStore.channels[logconfig.memberUnmuteLogChannel]
                            .sendMessage(texts.formatString("log.muted", mention, message.author.mention, reason))
                        logger.debug { "LOG message sent" }
                    } catch (e: Exception) {
                        logger.warn(e) { "${guild.name} (${guild.id}): Log error" }
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
                    textMessage(texts.formatString("muted", mention, reason))
                }
            }
        }
        return textMessage(texts.getStringOrKey("error"))
    }
}