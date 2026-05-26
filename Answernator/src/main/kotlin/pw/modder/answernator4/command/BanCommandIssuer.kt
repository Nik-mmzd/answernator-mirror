package pw.modder.answernator4.command

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import pw.modder.answernator4.interaction.MessageCommand
import pw.modder.answernator4.interaction.Option
import pw.modder.answernator4.interaction.description
import pw.modder.answernator4.interaction.getValue
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.interaction.optional
import pw.modder.answernator4.interaction.provideDelegate
import pw.modder.answernator4.interaction.string
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}
class BanCommandIssuer : MessageCommand() {
    override val name = "ban_author"
    override val bundleName = "v4.ban_author"
    override val defaultMemberPermissions = Permissions(Permission.BanMembers)
    override val dmPermission = false

    override suspend fun MessageCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferEphemeralResponse()
        val message = interaction.getTargetOrNull()
        if (message == null) {
            reply.respond {
                content = bundle.l("command.ban_author.no_message")
            }
            return
        }

        val interaction = message.interaction
        if (interaction == null) {
            reply.respond {
                content = bundle.l("command.ban_author.no_reference")
            }
            return
        }

        val guild = message.data.guildId.value
        if (guild == null) {
            reply.respond {
                content = bundle.l("command.ban_author.no_guild")
            }
            return
        }

        // TODO modal for duration and reason

        try {
            interaction.kord.rest.guild.addGuildBan(guild, interaction.user.id) {
                deleteMessageDuration = 1.days
                reason = bundle.l("command.ban_author.ban_reason_default").format(interaction.id, interaction.name)
            }
        } catch (e: Exception) {
            logger.warn(e) { "An error occurred while adding guild to ban" }
            reply.respond {
                content = bundle.l("command.ban_author.error")
            }
            return
        }

        reply.respond {
            content = bundle.l("command.ban_author.result")
                .format(interaction.user.id, interaction.id, interaction.name, interaction.type)
        }
    }
}
