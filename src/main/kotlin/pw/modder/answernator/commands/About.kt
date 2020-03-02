package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.serialization.UnstableDefault
import org.apache.commons.io.FileUtils
import pw.modder.answernator.utils.*

@UnstableDefault
class About: Command {
    override val name: String = "about"

    override suspend fun action(clientStore: ClientStore, message: Message) {
        val msg = com.jessecorbett.diskord.dsl.message {
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
            field("OS name", System.getProperty("os.name", "Unknown"), true)
            field("OS arch", System.getProperty("os.arch", "Unknown"), true)
            field("Uptime", Utils.getReadableUptime(), true)
        }
        clientStore.channels[message.channelId].sendMessage(msg.text, msg.embed())
    }
}