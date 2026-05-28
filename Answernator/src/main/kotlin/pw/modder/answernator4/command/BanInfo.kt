package pw.modder.answernator4.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.Option
import pw.modder.answernator4.interaction.description
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.interaction.userId

private val logger = KotlinLogging.logger {}
class BanInfo : ChatInputCommand() {
    override val name = "ban_info"
    override val bundleName = "v4.ban_info"
    override val dmPermission = false

    val target: Option<Snowflake> by userId().description("ban_info.target")

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferPublicResponse()
        val bundle = gbundle
        val target by option(target)

        val guildId = interaction.data.guildId.value
        if (guildId == null) {
            reply.respond {
                content = bundle.l("command.ban_info.no_guild")
            }
            return
        }

        val ban = try {
            interaction.kord.rest.guild.getGuildBan(guildId, target)
        } catch (e: Exception) {
            logger.warn(e) { "An error occurred while fetching a ban for $guildId member $target" }
            reply.respond {
                content = bundle.l("command.ban_info.no_ban")
            }
            return
        }

        reply.respond {
            content = bundle.l("command.ban_info.response")
                .format(ban.user.id, ban.user.username, ban.reason)
        }
    }
}
