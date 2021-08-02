package pw.modder.answernator.utils.extensions.bot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.on
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.extensions.kord.getMute
import pw.modder.answernator.utils.extensions.kord.mute
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

private val logger = KotlinLogging.logger {}
suspend fun Kord.muteService() {
    on<MemberJoinEvent> {
        val config = Db.getLogConfig(guildId) ?: return@on
        val role = Snowflake(config.muteRole ?: return@on)

        if (!Db.isMuted(guildId, member.id)) return@on
        member.addRole(role)
        logger.debug { "Member muted automatically: Guild ${guildId}, User ${member.username} ID ${member.id}" }
    }

    on<MemberUpdateEvent> {
        val config = Db.getLogConfig(guildId) ?: return@on
        val role = Snowflake(config.muteRole ?: return@on)
        val texts = CommandLocaleBundle("mute", Locale(config.lang))
        val hasRole = member.roleIds.any { it == role }

        if (hasRole && member.id == kord.selfId) {
            rest.channel.createMessage(Snowflake(config.memberMuteLogChannel ?: return@on)) {
                content = texts["muted.self"].format(member.mention)
            }
            return@on
        }

        val mute = member.getMute()
        // if isMuted and hasRole OR !isMuted and !hasRole
        if ((mute != null) == hasRole)
            return@on

        if (mute == null) {
            member.mute()
            if (config.isEnabled(Features.LOG_MUTE))
                rest.channel.createMessage(Snowflake(config.memberMuteLogChannel ?: return@on)) {
                    content = when(config.isEnabled(Features.MUTE_RANDOM_REASON)) {
                        true -> texts["muted.reason"].format(member.mention, texts.random("reason"))
                        false -> texts["muted"].format(member.mention)
                    }
                }
            return@on
        }

        transaction { mute.delete() }
        if (config.isEnabled(Features.LOG_UNMUTE)) {
            rest.channel.createMessage(Snowflake(config.memberUnmuteLogChannel ?: return@on)) {
                content = String.format(
                    texts["unmuted"],
                    member.mention
                )
            }
        }
    }
}