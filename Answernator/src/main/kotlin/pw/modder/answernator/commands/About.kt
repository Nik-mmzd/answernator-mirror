package pw.modder.answernator.commands

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import org.apache.commons.io.FileUtils
import pw.modder.answernator.utils.*
import pw.modder.answernator.utils.extensions.kord.authorId
import pw.modder.answernator.utils.extensions.kord.getColor
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class About: LocalizedCommand {
    override val name: String = "about"
    override val cmdType = Command.CommandGroup.OTHER

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle) {
        message.reply {
            embed {
                title = message.kord.getSelf().tag
                description = texts.getString("description") // "Third iteration of Answernator. Now in Kotlin!"

                if (message.authorId == Globals.config.author) {
                    field("OS", true) { System.getProperty("os.name", "Unknown") + ' ' + System.getProperty("os.arch", "Unknown") }
                    System.getenv("HOSTNAME") ?: System.getenv("COMPUTERNAME")?.also { field(texts.getString("hostname"), true) { it } }
                    System.getProperty("java.vendor")?.also { field(texts.getString("java.vendor"), true) { it } }
                    field(texts.getString("java.version"),true) { System.getProperty("java.version", "Unknown") }
                    field(texts.getString("heap.max"), true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()) }
                    field(texts.getString("heap"), true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory()) }
                    field(texts.getString("heap.used"), true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) }
                    field(texts.getString("heap.free"), true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory()) }
                    field(texts.getString("commands"), true) { CommandList.commands.size.toString() }
                    field(texts.getString("modules"), true) { CommandList.modules.joinToString("\n") { "${it.name}@${it.version}".trim('\n') } }
                    field(texts.getString("uptime"), true) { Utils.getReadableUptime() }
                }

                field(texts.getString("kotlin"), true) { KotlinVersion.CURRENT.toString() }
                field(texts.getString("library"), true) { Globals.getDependencyVersion("dev.kord", "kord-core") }
                field(texts.getString("bot"), true) { Globals.getDependencyVersion("pw.modder", "Answernator") }

                field(texts.getString("owner"), true) { "<@${Globals.config.author}>" }
                field(texts.getString("creator"), true) { "<@135017849604276224>" }

                field(texts.getString("source"), false) { texts.getString("source.link") }
                field(texts.getString("issues"), false) { texts.getString("issues.link") }
                field(texts.getString("invite"), false) { texts.getString("invite.link") }

                color = message.getGuildOrNull()?.getMemberOrNull(message.kord.selfId)?.getColor()
            }
            allowedMentions { repliedUser = false }
        }
    }
}