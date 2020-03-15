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
import java.util.*

private val logger = KotlinLogging.logger {  }
@UnstableDefault
class Mute: LocalizedCommand {
    override val name = "mute"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_MESSAGES
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)

    override fun check(message: Message, permissions: Permissions): Boolean {
        return super.check(message, permissions) && Db.guilds.get(message.guildId ?: return false).muteRole.isNotEmpty()
    }

    override suspend fun check(message: Message, guildClient: GuildClient?): Boolean {
        return super.check(message, guildClient) && Db.guilds.get(message.guildId ?: return false).muteRole.isNotEmpty()
    }

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val guildClient = bot.clientStore.guilds[message.guildId ?: return textMessage(texts.getStringOrKey("error"))]
        val guild = guildClient.getCached()
        val muteRole = Db.guilds.get(guild.id).muteRole

        if (muteRole.isEmpty()) return textMessage(texts.getStringOrKey("not.configured"))
        if (message.words.size < 2) return textMessage(texts.getStringOrKey("error"))
        message.usersMentioned.singleOrNull()?.run {
            val logconfig = Db.logs.get(guild.id)
            return when(guild.memberIsMuted(id)) {
                true -> {
                    guild.unmuteMember(id)
                    if (logconfig.memberUnmuteLogChannel.isNotEmpty()) try {
                        bot.clientStore.channels[logconfig.memberUnmuteLogChannel]
                            .sendMessage(texts.formatString("log.unmuted", mention, message.author.mention))
                    } catch (e: Exception) {
                        logger.warn(e) { "${guild.name} (${guild.id}): Log error" }
                    }
                    guildClient.removeMemberRole(muteRole, id)
                    textMessage(texts.formatString("unmuted", mention))
                }
                false -> {
                    guild.muteMember(id)
                    val reason = message.words.drop(2).joinToString(" ").ifEmpty {
                        val reasonCount = texts.getStringOrKey("reason.count").toIntOrNull() ?: 1
                        texts.getStringOrKey("reason.${Globals.random.nextInt(0, reasonCount)}")
                    }
                    if (logconfig.memberUnmuteLogChannel.isNotEmpty()) try {
                        bot.clientStore.channels[logconfig.memberUnmuteLogChannel]
                            .sendMessage(texts.formatString("log.muted", mention, message.author.mention, reason))
                    } catch (e: Exception) {
                        logger.warn(e) { "${guild.name} (${guild.id}): Log error" }
                    }
                    guildClient.addMemberRole(muteRole, id)
                    textMessage(texts.formatString("muted", mention, reason))
                }
            }
        }
        return textMessage(texts.getStringOrKey("error"))
    }
}