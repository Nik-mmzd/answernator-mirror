package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import mu.KotlinLogging
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.Db.memberIsMuted
import pw.modder.answernator.db.Db.muteMember
import pw.modder.answernator.db.Db.unmuteMember
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.UTF8Control
import pw.modder.answernator.utils.extensions.getStringOrKey
import java.util.*

private val logger = KotlinLogging.logger {}
@DiskordDsl
fun Bot.muteService() {
    userJoinedGuild {
        val memberId = it.user?.id ?: run {
            logger.error { "Mute Checker: ${it.guildId}, no user object, can't apply mute role!" }
            return@userJoinedGuild
        }
        val roleId = Db.guilds.get(it.guildId).muteRole.takeIf { it.isNotEmpty() } ?: return@userJoinedGuild
        val guildClient = clientStore.guilds[it.guildId]
        if (guildClient.memberIsMuted(memberId)) guildClient.addMemberRole(userId = memberId, roleId =  roleId)
        logger.debug { "Member muted automatically: Guild ${it.guildId}, User ${it.user?.username} ID ${it.user?.id}" }
    }

    guildMemberUpdated {
        val config = Db.guilds.get(it.guildId)
        val roleId = config.muteRole.takeIf { it.isNotEmpty() } ?: return@guildMemberUpdated
        val logConfig = Db.logs.get(it.guildId)
        val guild = clientStore.guilds[it.guildId]
        val texts = ResourceBundle.getBundle("locale.mute", UTF8Control())

        if (it.roles.any { it == roleId } && !guild.memberIsMuted(it.user.id)) {
            try {
                logConfig.memberMuteLogChannel.takeIf { it.isNotEmpty() }?.run {
                    clientStore.channels[this].sendMessage(
                        String.format(
                            texts.getStringOrKey("mute.muted"),
                            it.user.mention,
                            texts.getStringOrKey("mute.reason.${Globals.random.nextInt(0, texts.getStringOrKey("mute.reason.count").toIntOrNull() ?: 1)}")
                        ))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Logger: Error while auto-muting by role update" }
            }
            guild.addMemberRole(it.user.id, roleId)
            guild.muteMember(it.user.id)

            return@guildMemberUpdated
        }

        if (it.roles.none { it == roleId } && guild.memberIsMuted(it.user.id)) {
            try {
                logConfig.memberUnmuteLogChannel.takeIf { it.isNotEmpty() }?.run {
                    clientStore.channels[this].sendMessage(
                        String.format(
                            texts.getStringOrKey("mute.unmuted"),
                            it.user.mention
                        ))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Logger: Error while auto-unmuting by role update" }
            }
            guild.removeMemberRole(it.user.id, roleId)
            guild.unmuteMember(it.user.id)
        }
    }
}