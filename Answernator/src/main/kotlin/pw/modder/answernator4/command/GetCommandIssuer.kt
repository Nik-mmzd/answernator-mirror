package pw.modder.answernator4.command

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import pw.modder.answernator4.interaction.MessageCommand
import pw.modder.answernator4.interaction.l

class GetCommandIssuer : MessageCommand() {
    override val name = "get_author"
    override val bundleName = "v4.get_author"
    override val defaultMemberPermissions = Permissions(Permission.ManageMessages)
    override val dmPermission = false

    override suspend fun MessageCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferEphemeralResponse()
        val message = interaction.getTargetOrNull()
        if (message == null) {
            reply.respond {
                content = bundle.l("command.get_author.no_message")
            }
            return
        }

        val interaction = message.interaction
        if (interaction == null) {
            reply.respond {
                content = bundle.l("command.get_author.result_no_reference")
            }
            return
        }

        reply.respond {
            content = bundle.l("command.get_author.result")
                .format(interaction.user.id, interaction.id, interaction.name, interaction.type)
        }
    }
}
