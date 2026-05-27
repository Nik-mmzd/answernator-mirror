package pw.modder.answernator4.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import org.apache.commons.io.FileUtils
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.kord.timestampNow
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.InteractionCommandList
import pw.modder.answernator4.interaction.l

class BotInfo : ChatInputCommand() {
    override val name = "bot_info"
    override val bundleName = "v4.debug"

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferPublicResponse()

        reply.respond { embed {
            title = interaction.kord.getSelf().tag
            description = bundle.l("info.description") // "Third iteration of Answernator. Now in Kotlin!"

            if (interaction.user.id.toString() == Globals.config.author) {
                field("OS", true) { System.getProperty("os.name", "Unknown") + ' ' + System.getProperty("os.arch", "Unknown") }
                System.getenv("HOSTNAME") ?: System.getenv("COMPUTERNAME")?.also { field(bundle.l("hostname"), true) { it } }
                System.getProperty("java.vendor")?.also { field(bundle.l("java.vendor"), true) { it } }
                field(bundle.l("java.version"),true) { System.getProperty("java.version", "Unknown") }
                field(bundle.l("heap.max"), true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()) }
                field(bundle.l("heap"), true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory()) }
                field(bundle.l("heap.used"), true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) }
                field(bundle.l("heap.free"), true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory()) }
                field(bundle.l("commands"), true) { InteractionCommandList.commands.size.toString() }
//                field(bundle.l("modules"), true) { CommandList.modules.joinToString("\n") { "${it.name}@${it.version}".trim('\n') } }
                field(bundle.l("uptime"), true) { Utils.getReadableUptime() }
            }

            field(bundle.l("kotlin"), true) { KotlinVersion.CURRENT.toString() }
            field(bundle.l("library"), true) { Globals.getDependencyVersion("dev.kord", "kord-core") }
            field(bundle.l("bot"), true) { Globals.getDependencyVersion("pw.modder", "Answernator") }

            field(bundle.l("owner"), true) { Globals.config.author.toUserMention() }
            field(bundle.l("creator"), true) { "135017849604276224".toUserMention() }

            field(bundle.l("source"), false) { bundle.l("source.link") }
            field(bundle.l("issues"), false) { bundle.l("issues.link") }
//            field(bundle.l["invite"], false) { bundle.l["invite.link"] }

            timestampNow()
        } }
    }
}
