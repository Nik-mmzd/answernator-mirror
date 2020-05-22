package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import org.apache.commons.io.FileUtils
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.bot.getMe
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

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

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        val me = bot.getMe()
        return dslmessage {
            title = me.username
            description = "Third iteration of Answernator. Now in Kotlin!"
            System.getProperty("java.vendor")?.also { field("Java Vendor", it, true) }
            field("Java Version", System.getProperty("java.version", "Unknown"), true)
            field("Kotlin", KotlinVersion.CURRENT.toString(), true)
            field("Diskord", Globals.getDependencyVersion("com.jessecorbett", "diskord-jvm"), true)
            field("Bot version", Globals.getDependencyVersion("pw.modder", "Answernator"), true)
            field("Commands", CommandList.commands.size.toString(), true)
            field("Modules", CommandList.modules.joinToString("\n") { "${it.name}@${it.version}" }, true)
            field("Commands prefix", Globals.config.prefix.toString(), true)
            field("Owner", "<@${Globals.config.author}>", true)
            field("Creator", "<@135017849604276224>", true)
            field("Max heap size", FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()), true)
            field("Current heap size", FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory()), true)
            field("Heap used", FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()), true)
            field("Heap free", FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory()), true)
            field("OS", System.getProperty("os.name", "Unknown") + ' ' + System.getProperty("os.arch", "Unknown"), true)
            field("Uptime", Utils.getReadableUptime(), true)
        }
    }
}