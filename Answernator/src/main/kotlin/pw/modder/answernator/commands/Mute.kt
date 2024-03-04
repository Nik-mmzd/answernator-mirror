package pw.modder.answernator.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.extractMentionedId
import pw.modder.answernator.utils.extensions.kord.getMute
import pw.modder.answernator.utils.extensions.kord.isAdmin
import pw.modder.answernator.utils.extensions.kord.mute
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

private val logger = KotlinLogging.logger {  }

class Mute: LocalizedGuildCommand {
    override val name = "mute"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.ManageMessages
    override val cmdType = Command.CommandGroup.MODER
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)

    override val requiredPermission: Permission? = Permission.ManageRoles

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        val muteRole = config.muteRole

        logger.debug { "Checking configs" }
        if (muteRole == null) {
            message.reply(texts["not.configured"])
            return
        }
        if (args.isEmpty()) {
            message.reply(texts.error())
            return
        }

        val mentionedUserId = args.first().extractMentionedId()
        if (mentionedUserId == null) {
            message.reply(texts.error())
            return
        }

        val mentionedUser = message.mentionedUserBehaviors.find { it.id.toString() == mentionedUserId }?.asMemberOrNull(guild.id)
        if (mentionedUser == null) {
            message.reply(texts.error())
            return
        }

        with(mentionedUser) {
            if (message.kord.selfId == mentionedUser.id) {
                message.reply(texts["muted.self"].format(mention)) // easter egg
                return
            }
            if (mentionedUser.isAdmin()) {
                message.reply(texts["error.whitelisted"].format(mention))
                return
            }

            if (Db.isMuted(guildId, id)) {
                logger.debug { "Member is muted" }
                message.reply(texts["muted.already"].format(mention))
                return
            }

            logger.debug { "Member is not muted" }
            val reason = args.drop(1).joinToString(" ").ifEmpty {
                when(config.isEnabled(Features.MUTE_RANDOM_REASON)) {
                    true -> texts.random("reason")
                    false -> null
                }
            }
            logger.debug { "Got mute reason" }

            if (config.memberMuteLogChannel != null) try {
                message.kord.rest.channel.createMessage(Snowflake(config.memberMuteLogChannel!!)) {
                    content = when(reason) {
                        null -> texts["log.muted.noreason"].format(mention, message.author!!)
                        else -> texts["log.muted"].format(mention, message.author!!.mention, reason)
                    }
                }
                logger.debug { "LOG message sent" }
            } catch (e: Exception) {
                logger.warn(e) { "${guild.name} (${guild.id}): Log error" }
            }

            val mute = mentionedUser.mute()
            logger.debug { "Member muted in DB" }

            try {
                addRole(Snowflake(muteRole), reason)
                logger.debug { "Added mute role" }
            } catch (e: Exception) {
                logger.error(e) { "Error adding mute role for user $username at ${guild.name}, role id $muteRole. Member is UNMUTED in DB" }
                transaction { mute.delete() }
                throw e
            }

            message.reply(texts["muted"].format(mention))
        }
    }
}

class Unmute: LocalizedGuildCommand {
    override val name = "unmute"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.ManageMessages
    override val cmdType = Command.CommandGroup.MODER
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val requiredPermission: Permission? = Permission.ManageRoles

    override fun getTexts(locale: Locale): CommandLocaleBundle {
        return CommandLocaleBundle("mute", locale, javaClass.classLoader)
    }

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        val muteRole = config.muteRole

        logger.debug { "Checking configs" }
        if (muteRole == null) {
            message.reply(texts["not.configured"])
            return
        }
        if (args.isEmpty()) {
            message.reply(texts.error())
            return
        }

        val mentionedUserId = args.first().extractMentionedId()
        if (mentionedUserId == null) {
            message.reply(texts.error())
            return
        }

        val mentionedUser = message.mentionedUserBehaviors.find { it.id.toString() == mentionedUserId }?.asMemberOrNull(guild.id)
        if (mentionedUser == null) {
            message.reply(texts.error())
            return
        }

        with(mentionedUser) {
            val mute = getMute()

            if (mute == null) {
                logger.debug { "Member is not muted" }
                message.reply(texts["unmuted.already"].format(mention))
                return
            }

            logger.debug { "Member is muted" }
            transaction { mute.delete() }
            logger.debug { "Unmuted user in DB" }

            val reason = args.drop(1).joinToString(" ").ifEmpty { null }

            if (config.memberUnmuteLogChannel != null) try {
                message.kord.rest.channel.createMessage(Snowflake(config.memberUnmuteLogChannel!!)) {
                    content = texts["log.unmuted"].format(mention, message.author!!.mention)
                }
                logger.debug { "LOG message sent" }
            } catch (e: Exception) {
                logger.warn(e) { "${guild.name} (${guild.id}): Log error" }
            }

            removeRole(Snowflake(muteRole), reason)

            message.reply(texts["unmuted"].format(mention))
        }
    }
}