package pw.modder.answernator4.command

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.optional.map
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import pw.modder.answernator.utils.extensions.kord.timestampNow
import pw.modder.answernator4.interaction.ChatInputCommand
import dev.kord.common.asJavaLocale
import pw.modder.answernator4.interaction.boolean
import pw.modder.answernator4.interaction.default
import pw.modder.answernator4.interaction.description
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.interaction.name

class DebugInfo : ChatInputCommand() {
    override val name = "debug_info"
    override val bundleName = "v4.debug"
    override val defaultMemberPermissions = Permissions(Permission.ManageGuild)

    val public by boolean().name("debug.public").description("debug.public.description").default(false)

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val public by option(public)
        val reply = if (public)
            interaction.deferPublicResponse()
        else
            interaction.deferEphemeralResponse()

        val bundle = if (public) gbundle else bundle

        reply.respond { embed {
            title = bundle.l("command.debug.title")
            field {
                name = bundle.l("command.debug.field.locale")
                value = "${interaction.data.locale.value ?: "null"} => ${interaction.data.locale.map { it.asJavaLocale() }.value ?: "null"}"
            }
            field {
                name = bundle.l("command.debug.field.guildLocale")
                value = "${interaction.data.guildLocale.value ?: "null"} => ${interaction.data.guildLocale.map { it.asJavaLocale() }.value ?: "null"}"
            }
            field {
                name = bundle.l("command.debug.field.member")
                value = interaction.data.member.map { "Member ID ${it.userId}, guild ID ${it.guildId}" }.value ?: "null"
            }
            field {
                name = bundle.l("command.debug.field.user")
                value = interaction.data.user.map { "User ${it.username} ID ${it.id}" }.value ?: "null"
            }
            field {
                name = bundle.l("command.debug.field.channel")
                value = interaction.data.channel.map { "${it.type} channel ${it.name.value ?: "UNKNOWN"} ID ${it.id}, guild ${it.guildId.value ?: "none"}" }.value ?: "null"
            }
            timestampNow()
        } }
    }
}
