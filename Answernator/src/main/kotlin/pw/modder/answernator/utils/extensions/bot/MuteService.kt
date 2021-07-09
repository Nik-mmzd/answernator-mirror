package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import mu.KotlinLogging
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Mute
import pw.modder.answernator.db.guild.Mutes
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

        if (!transaction { config.mutes }.any { it.memberId == memberId }) return@userJoinedGuild

        clientStore.guilds[it.guildId].addMemberRole(userId = memberId, roleId = config.muteRole!!)
        logger.debug { "Member muted automatically: Guild ${it.guildId}, User ${it.user?.username} ID ${it.user?.id}" }
    }

    guildMemberUpdated { update ->
        val config = Db.getConfig(update.guildId)
        val roleId = config.muteRole ?: return@guildMemberUpdated

        val mute = transaction { config.mutes }.firstOrNull()

        val client = clientStore.guilds[update.guildId]
        val texts = CommandLocaleBundle("mute", Locale(config.lang))

        if (update.roles.any { it == roleId } && mute == null) {
            try {
                config.memberMuteLogChannel?.run {
                    if (update.user.id == getMe().id) {
                        clientStore.channels[this].sendMessage(
                            texts.formatString("muted.self", update.user.mention)
                        )
                        return@guildMemberUpdated
                    }

                    clientStore.channels[this].sendMessage(
                        texts.formatString(
                            "muted",
                            update.user.mention,
                            texts.getRandomString("reason")
                        )
                    )
                }
            } catch (e: Exception) {
                logger.warn(e) { "Logger: Error while auto-muting by role update" }
            }
            client.addMemberRole(update.user.id, roleId)
            Mute.new {
                guild = update.guildId
                memberId = update.user.id
            }

            return@guildMemberUpdated
        }

        if (update.roles.none { it == roleId } && mute != null) {
            try {
                config.memberUnmuteLogChannel?.run {
                    clientStore.channels[this].sendMessage(
                        String.format(
                            texts.getString("unmuted"),
                            update.user.mention
                        ))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Logger: Error while auto-unmuting by role update" }
            }
            client.removeMemberRole(update.user.id, roleId)
            transaction { mute.delete() }
        }
    }
}