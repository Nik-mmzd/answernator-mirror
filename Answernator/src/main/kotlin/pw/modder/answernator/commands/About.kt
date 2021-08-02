package pw.modder.answernator.commands

import dev.kord.core.entity.Message
import org.apache.commons.io.FileUtils
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.*
import pw.modder.answernator.utils.extensions.kord.getColor
import pw.modder.answernator.utils.extensions.kord.isFromBotAuthor
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import pw.modder.answernator.utils.extensions.kord.timestampNow
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class About: LocalizedCommand {
    override val name: String = "about"
    override val cmdType = Command.CommandGroup.OTHER

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle, config: Config?) {
        message.replyEmbed {
            title = message.kord.getSelf().tag
            description = texts["description"] // "Third iteration of Answernator. Now in Kotlin!"

            if (message.isFromBotAuthor()) {
                field("OS", true) { System.getProperty("os.name", "Unknown") + ' ' + System.getProperty("os.arch", "Unknown") }
                System.getenv("HOSTNAME") ?: System.getenv("COMPUTERNAME")?.also { field(texts["hostname"], true) { it } }
                System.getProperty("java.vendor")?.also { field(texts["java.vendor"], true) { it } }
                field(texts["java.version"],true) { System.getProperty("java.version", "Unknown") }
                field(texts["heap.max"], true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()) }
                field(texts["heap"], true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory()) }
                field(texts["heap.used"], true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) }
                field(texts["heap.free"], true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory()) }
                field(texts["commands"], true) { CommandList.commands.size.toString() }
                field(texts["modules"], true) { CommandList.modules.joinToString("\n") { "${it.name}@${it.version}".trim('\n') } }
                field(texts["uptime"], true) { Utils.getReadableUptime() }
            }

            field(texts["kotlin"], true) { KotlinVersion.CURRENT.toString() }
            field(texts["library"], true) { Globals.getDependencyVersion("dev.kord", "kord-core") }
            field(texts["bot"], true) { Globals.getDependencyVersion("pw.modder", "Answernator") }

            field(texts["owner"], true) { Globals.config.author.toUserMention() }
            field(texts["creator"], true) { "135017849604276224".toUserMention() }

            field(texts["source"], false) { texts["source.link"] }
            field(texts["issues"], false) { texts["issues.link"] }
            field(texts["invite"], false) { texts["invite.link"] }

            color = message.getGuildOrNull()?.getMemberOrNull(message.kord.selfId)?.getColor()
            timestampNow()
        }
    }
}