package pw.modder.answernatorCommandsExtension.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.mention
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import pw.modder.answernatorCommandsExtension.utils.UbuntuWord
import pw.modder.answernator.utils.Command
import java.util.*

@UnstableDefault
class Ubuntu: Command {
    override val name: String = "ubuntu"
    private val data = Json(JsonConfiguration.Default)
        .parse(UbuntuWord.serializer().list, javaClass.classLoader.getResourceAsStream("ubuntu.json").reader().readText())

    @UnstableDefault
    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        return textMessage("${message.author.mention}, ${data.random().run { first.random() + ' ' + second.random() }}")
    }
}