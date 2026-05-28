package pw.modder.answernator4.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.embed
import org.apache.commons.io.FileUtils
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.kord.timestampNow
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator4.BuildConfig
import pw.modder.answernator4.Env
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.InteractionCommandList
import pw.modder.answernator4.interaction.l

class BotInfo : ChatInputCommand() {
    override val name = "bot_info"
    override val bundleName = "v4.debug"

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferPublicResponse()
        val bundle = gbundle

        reply.respond {
            embed {
                title = interaction.kord.getSelf().tag
                description = bundle.l("command.info.description")

                if (interaction.user.id == Env.BOT_OWNER_SNOWFLAKE) {
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
                    field(bundle.l("command.info.commands"), true) { InteractionCommandList.commands.size.toString() }
//                field(bundle.l("modules"), true) { CommandList.modules.joinToString("\n") { "${it.name}@${it.version}".trim('\n') } }
                    field(bundle.l("command.info.uptime"), true) { Utils.getReadableUptime() }
                }

                field(bundle.l("command.info.version.kotlin"), true) { KotlinVersion.CURRENT.toString() }
                field(bundle.l("command.info.version.library"), true) { BuildConfig.KORD_VERSION }
                field(bundle.l("command.info.version.bot"), true) { BuildConfig.APP_VERSION }

                field(bundle.l("command.info.owner"), true) { Env.BOT_OWNER_ID.toUserMention() }
                field(bundle.l("command.info.creator"), true) { BuildConfig.APP_CREATOR_ID.toUserMention() }

                timestampNow()
            }

            if (BuildConfig.APP_SOURCE_URL.isNotBlank() || BuildConfig.APP_ISSUES_URL.isNotBlank() || Env.BOT_INVITE_LINK.isNotBlank())
                actionRow {
                    if (BuildConfig.APP_SOURCE_URL.isNotBlank())
                        linkButton(BuildConfig.APP_SOURCE_URL) {
                            label = bundle.l("command.info.links.source")
                        }
                    if (BuildConfig.APP_ISSUES_URL.isNotBlank())
                        linkButton(BuildConfig.APP_ISSUES_URL) {
                            label = bundle.l("command.info.links.issues")
                        }
                    if (Env.BOT_INVITE_LINK.isNotBlank())
                        linkButton(Env.BOT_INVITE_LINK) {
                            label = bundle.l("command.info.links.invite")
                        }
                }
        }
    }
}
