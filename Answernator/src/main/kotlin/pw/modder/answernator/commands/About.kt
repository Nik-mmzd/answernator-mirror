package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.dsl.message as dslmessage
import kotlinx.serialization.UnstableDefault
import org.apache.commons.io.FileUtils
import pw.modder.answernator.utils.*
import java.util.*

@UnstableDefault
class About: Command {
    override val name: String = "about"
    override val userGroup = Command.UserGroup.ADMIN

    override fun getHelp(locale: Locale): String? {
        return "Shows some technical information about the bot. Usage: `$name`. Not localized."
    }

    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        return dslmessage {
            title = "Answernator"
            description = "Third iteration of Answernator. Now in Kotlin!"
            System.getProperty("java.vendor")?.also { field("Java Vendor", it, true) }
            field("Java Version", System.getProperty("java.version", "Unknown"), true)
            field("Kotlin", KotlinVersion.CURRENT.toString(), true)
            field("Diskord", Dependencies.getVersion("com.jessecorbett", "diskord-jvm"), true)
            field("Bot version", Dependencies.getVersion("pw.modder", "Answernator"), true)
            field("Commands", CommandList.commands.size.toString(), true)
            field("Commands prefix", GlobalConfig.get().prefix.toString(), true)
            field("Owner", "<@${GlobalConfig.get().author}>", true)
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