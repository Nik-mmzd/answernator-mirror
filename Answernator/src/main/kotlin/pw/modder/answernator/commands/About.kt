package pw.modder.answernator.commands

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import org.apache.commons.io.FileUtils
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.Utils
import java.util.*

class About: Command {
    override val name: String = "about"
    override val userGroup = Command.UserGroup.ADMIN

    override fun getHelp(locale: Locale): String? {
        return "Shows some technical information about the bot. Usage: `$name`. Not localized."
    }

    override fun getDescription(locale: Locale): String? {
        return "technical bot information"
    }

    override val cmdType = Command.CommandGroup.OWNER

    override suspend fun action(message: Message, args: List<String>, locale: Locale) {
        message.reply {
            embed {
                title = message.kord.getSelf().tag
                description = "Third iteration of Answernator. Now in Kotlin!"

                System.getProperty("java.vendor")?.also { field("Java Vendor", true) { it } }
                field("Java Version",true) { System.getProperty("java.version", "Unknown") }
                field("Kotlin", true) { KotlinVersion.CURRENT.toString() }
                field("Kord", true) { Globals.getDependencyVersion("dev.kord", "kord-core") }
                field("Bot version", true) { Globals.getDependencyVersion("pw.modder", "Answernator") }
                field("Commands", true) { CommandList.commands.size.toString() }
                field("Modules", true) { CommandList.modules.joinToString("\n") { "${it.name}@${it.version}".trim('\n') } }
                field("Commands prefix", true) { Globals.config.prefix.toString() }
                field("Owner", true) { "<@${Globals.config.author}>" }
                field("Creator", true) { "<@135017849604276224>" }
                field("Max heap size", true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()) }
                field("Current heap size", true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory()) }
                field("Heap used", true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) }
                field("Heap free", true) { FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory()) }
                field("OS", true) { System.getProperty("os.name", "Unknown") + ' ' + System.getProperty("os.arch", "Unknown") }
                field("Uptime", true) { Utils.getReadableUptime() }
            }
            allowedMentions { repliedUser = false }
        }
    }
}