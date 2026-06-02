package pw.modder.answernator4.command

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.kodein.di.DI
import pw.modder.answernator4.interaction.MessageCommand
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.interaction.modal.Modal
import pw.modder.answernator4.interaction.modal.SelectChoice
import pw.modder.answernator4.interaction.modal.getValue
import pw.modder.answernator4.interaction.modal.provideDelegate
import pw.modder.answernator4.interaction.modal.radioGroup
import pw.modder.answernator4.interaction.modal.showModal
import pw.modder.answernator4.interaction.modal.textField
import java.util.ResourceBundle
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}
class BanCommandIssuer(di: DI) : MessageCommand(di) {
    override val name = "ban_author"
    override val bundleName = "v4.ban_author"
    override val defaultMemberPermissions = Permissions(Permission.BanMembers)
    override val dmPermission = false

    override suspend fun MessageCommandInteractionCreateEvent.execute() {
        val message = interaction.getTargetOrNull()
        if (message == null) {
            interaction.respondEphemeral {
                content = bundle.l("command.ban_author.no_message")
            }
            return
        }

        val msgInteraction = message.interaction
        if (msgInteraction == null) {
            interaction.respondEphemeral {
                content = bundle.l("command.ban_author.no_reference")
            }
            return
        }

        val guild = message.data.guildId.value ?: interaction.data.member.value?.guildId
        if (guild == null) {
            interaction.respondEphemeral {
                content = bundle.l("command.ban_author.no_guild")
            }
            return
        }

        val defaultReason = gbundle.l("command.ban_author.ban_reason_default").format(msgInteraction.id, msgInteraction.name)
        val modal = BanModal(bundle, msgInteraction.user.id, defaultReason)
        val modalReply = interaction.showModal(modal)

        if (modalReply == null) {
            interaction.respondEphemeral {
                content = bundle.l("command.ban_author.cancelled")
            }
            return
        }
        val reply = modalReply.interaction.deferEphemeralResponse()

        val reason = modalReply[modal.reason]
        val duration = modalReply[modal.duration].toIntOrNull()?.hours

        try {
            logger.info { "Banning user ${msgInteraction.user.id} from ${guild}, reason: $reason, cleanup: $duration" }
            msgInteraction.kord.rest.guild.addGuildBan(guild, msgInteraction.user.id) {
                deleteMessageDuration = duration
                this.reason = reason
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
                .format(msgInteraction.user.id, msgInteraction.id, msgInteraction.name, msgInteraction.type)
        }
    }

    class BanModal(bundle: ResourceBundle, userId: Snowflake, defaultReason: String) : Modal(bundle.l("command.ban_author.modal.title").format(userId)) {
        val reason by textField(
            label = bundle.l("command.ban_author.modal.reason"),
            style = TextInputStyle.Paragraph,
            defaultValue = defaultReason,
        )

        val duration by radioGroup(
            label = bundle.l("command.ban_author.modal.duration"),
            choices = listOf(
                SelectChoice(bundle.l("command.ban_author.modal.duration.1hour"), "1"),
                SelectChoice(bundle.l("command.ban_author.modal.duration.6hours"), "6"),
                SelectChoice(bundle.l("command.ban_author.modal.duration.12hours"), "12"),
                SelectChoice(bundle.l("command.ban_author.modal.duration.24hours"), "24", default = true),
                SelectChoice(bundle.l("command.ban_author.modal.duration.none"), "0"),
            )
        )
    }
}
