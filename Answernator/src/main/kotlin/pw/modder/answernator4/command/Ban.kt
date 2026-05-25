package pw.modder.answernator4.command

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import pw.modder.answernator4.interaction.*

class Ban : ChatInputCommand() {
    override val name = "ban"
    override val bundleName = "slash.ban"
    override val description = "command.ban.description".asLocaleKey()
    override val defaultMemberPermissions = Permissions(Permission.BanMembers)

    val target: Option<Snowflake> by userId().description("command.ban.target.description")
    val reason: Option<String?> by string().description("command.ban.reason.description").optional()

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val target: Snowflake by option(target)
        val reason: String? by option(reason)

        val banMessage = bundle.l("command.ban.success")

        interaction.respondEphemeral {
            content = banMessage
        }
    }
}
