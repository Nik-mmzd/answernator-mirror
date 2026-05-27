package pw.modder.answernator4.command

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.optional.map
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import pw.modder.answernator.utils.extensions.kord.timestampNow
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.SUPPORTED_LOCALES
import pw.modder.answernator4.interaction.boolean
import pw.modder.answernator4.interaction.default
import pw.modder.answernator4.interaction.description
import pw.modder.answernator4.interaction.getValue
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

        reply.respond { embed {
            title = bundle.l("command.debug.title")
            field {
                name = bundle.l("command.debug.field.locale")
                value = "${interaction.data.locale} => ${interaction.data.locale.map { SUPPORTED_LOCALES[it] ?: java.util.Locale.ROOT }.value ?: java.util.Locale.ROOT}"
            }
            field {
                name = bundle.l("command.debug.field.guildLocale")
                value = "${interaction.data.guildLocale} => ${interaction.data.guildLocale.map { SUPPORTED_LOCALES[it] ?: java.util.Locale.ROOT }.value ?: java.util.Locale.ROOT}"
            }
            field {
                name = bundle.l("command.debug.field.member")
                value = interaction.data.member.map { "Member ID ${it.userId}, guild ID ${it.guildId}" }.toString()
            }
            field {
                name = bundle.l("command.debug.field.user")
                value = interaction.data.user.map { "User ${it.username} ID ${it.id}" }.toString()
            }
            field {
                name = bundle.l("command.debug.field.channel")
                value = interaction.data.channel.map { "${it.type} channel ${it.name} ID ${it.id}, guild ${it.guildId}" }.toString()
            }
            timestampNow()
        } }
    }
}
