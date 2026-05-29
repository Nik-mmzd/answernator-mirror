package pw.modder.answernator4.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.embed
import dev.kord.rest.builder.message.modify.InteractionResponseModifyBuilder
import org.apache.commons.io.FileUtils
import org.kodein.di.DI
import org.kodein.di.instance
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.kord.timestampNow
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator4.BuildConfig
import pw.modder.answernator4.Env
import pw.modder.answernator4.di.KodeinModuleList
import pw.modder.answernator4.di.KodeinModuleProvider
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.CommandRegistry
import pw.modder.answernator4.interaction.button.ButtonField
import pw.modder.answernator4.interaction.button.ButtonGroup
import pw.modder.answernator4.interaction.button.button
import pw.modder.answernator4.interaction.button.linkButton
import pw.modder.answernator4.interaction.button.renderButtons
import pw.modder.answernator4.interaction.button.visibleIf
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.kord.getSelfCached
import java.util.ResourceBundle

class BotInfo(di: DI) : ChatInputCommand(di) {
    override val name = "bot_info"
    override val bundleName = "v4.debug"

    override val buttons = Buttons()

    private fun InteractionResponseModifyBuilder.render(bundle: ResourceBundle, isOwner: Boolean, self: User) {
        embed {
            title = self.tag
            description = bundle.l("command.info.description")

            if (isOwner) {
                val runtime = Runtime.getRuntime()

                field(bundle.l("command.info.os"), true) { Env.OS_STRING }
                Env.HOSTNAME
                    ?.also { field(bundle.l("command.info.hostname"), true) { it } }
                Env.JRE_VENDOR
                    ?.also { field(bundle.l("command.info.java.vendor"), true) { it } }
                field(bundle.l("command.info.java.version"),true) { Env.JRE_VERSION ?: "Unknown" }
                field(bundle.l("command.info.heap.max"), true) { FileUtils.byteCountToDisplaySize(runtime.maxMemory()) }
                field(bundle.l("command.info.heap"), true) { FileUtils.byteCountToDisplaySize(runtime.totalMemory()) }
                field(bundle.l("command.info.heap.used"), true) { FileUtils.byteCountToDisplaySize(runtime.totalMemory() - Runtime.getRuntime().freeMemory()) }
                field(bundle.l("command.info.heap.free"), true) { FileUtils.byteCountToDisplaySize(runtime.freeMemory()) }
                field(bundle.l("command.info.commands"), true) {
                    val registry: CommandRegistry by di.instance()
                    registry.commands.size.toString()
                }
                field(bundle.l("command.info.modules"), inline = true) {
                    KodeinModuleList.providers.joinToString(separator = "\n") { "${it.name}@${it.version}" }
                }
                field(bundle.l("command.info.uptime"), true) { Utils.getReadableUptime() }
            }

            field(bundle.l("command.info.version.kotlin"), true) { KotlinVersion.CURRENT.toString() }
            field(bundle.l("command.info.version.library"), true) { BuildConfig.KORD_VERSION }
            field(bundle.l("command.info.version.bot"), true) { BuildConfig.APP_VERSION }

            field(bundle.l("command.info.owner"), true) { Env.BOT_OWNER_ID.toUserMention() }
            field(bundle.l("command.info.creator"), true) { BuildConfig.APP_CREATOR_ID.toUserMention() }

            timestampNow()
        }

        actionRow {
            renderButtons(buttons, baseId = "cmd:$effectiveName", bundle = bundle)
        }
    }

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferPublicResponse()
        val isAdmin = interaction.user.id == Env.BOT_OWNER_SNOWFLAKE
        val self = interaction.kord.getSelfCached()

        reply.respond {
            render(gbundle, isAdmin, self)
        }
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
        if (button != buttons.refresh) return

        val reply = interaction.deferPublicMessageUpdate()
        val isAdmin = interaction.user.id == Env.BOT_OWNER_SNOWFLAKE
        val self = interaction.kord.getSelfCached()
        reply.edit {
            render(gbundle, isAdmin, self)
        }
    }

    class Buttons : ButtonGroup() {
        val refresh by button(
            style = ButtonStyle.Secondary,
            emoji = DiscordPartialEmoji(name = "\uD83D\uDD04")
        )
        val source by linkButton(BuildConfig.APP_SOURCE_URL, label = "command.info.links.source")
            .visibleIf(BuildConfig.APP_SOURCE_URL.isNotBlank())
        val issues by linkButton(BuildConfig.APP_ISSUES_URL, label = "command.info.links.issues")
            .visibleIf(BuildConfig.APP_ISSUES_URL.isNotBlank())
        val invite by linkButton(Env.BOT_INVITE_LINK, label = "command.info.links.invite")
            .visibleIf(Env.BOT_INVITE_LINK.isNotBlank())
    }
}
