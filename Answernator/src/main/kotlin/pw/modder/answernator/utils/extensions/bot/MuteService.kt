package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.db.guild.Mute
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

private val logger = KotlinLogging.logger {}
@DiskordDsl
fun Bot.muteService() {
    userJoinedGuild {
        val memberId = it.user?.id ?: run {
            logger.error { "Mute Checker: ${it.guildId}, no user object, can't apply mute role!" }
            return@userJoinedGuild
        }
        val config = Db.getGuildConfig(it.guildId)
        if (config.muteRole == null) return@userJoinedGuild

        if (!Db.isMuted(it.guildId, memberId)) return@userJoinedGuild

        clientStore.guilds[it.guildId].addMemberRole(userId = memberId, roleId = config.muteRole!!)
        logger.debug { "Member muted automatically: Guild ${it.guildId}, User ${it.user?.username} ID ${it.user?.id}" }
    }

    guildMemberUpdated { update ->
        val config = Db.getConfig(update.guildId)
        val roleId = config.muteRole ?: return@guildMemberUpdated

        val client = clientStore.guilds[update.guildId]
        val texts = CommandLocaleBundle("mute", Locale(config.lang))

        val mute = Db.getMute(update.guildId, update.user.id)

        if (update.roles.any { it == roleId } && mute == null) {
            if (config.isEnabled(Features.LOG_MUTE) && config.memberMuteLogChannel != null) {
                if (isMe(update.user)) {
                    clientStore.channels[config.memberMuteLogChannel!!].sendMessage(
                        texts.formatString("muted.self", update.user.mention)
                    )
                    return@guildMemberUpdated
                }
            }

            client.addMemberRole(update.user.id, roleId)
            Mute.new {
                guild = update.guildId
                memberId = update.user.id
            }
            clientStore.channels[config.memberMuteLogChannel!!].sendMessage(
                texts.formatString(
                    "muted",
                    update.user.mention,
                    texts.getRandomString("reason")
                )
            )

            return@guildMemberUpdated
        }

        if (update.roles.none { it == roleId } && mute != null) {
            client.removeMemberRole(update.user.id, roleId)
            transaction { mute.delete() }
            if (config.isEnabled(Features.LOG_UNMUTE) && config.memberUnmuteLogChannel != null) {
                clientStore.channels[config.memberUnmuteLogChannel!!].sendMessage(
                    String.format(
                        texts.getString("unmuted"),
                        update.user.mention
                    ))
            }
        }
    }
}