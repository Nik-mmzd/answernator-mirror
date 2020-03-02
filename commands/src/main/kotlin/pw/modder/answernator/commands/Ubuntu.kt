package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import pw.modder.answernator.commands.utils.UbuntuWord
import pw.modder.answernator.utils.Command

@UnstableDefault
class Ubuntu: Command {
    override val name: String = "ubuntu"
    private val data = Json(JsonConfiguration.Default)
        .parse(UbuntuWord.serializer().list, javaClass.classLoader.getResourceAsStream("ubuntu.json").reader().readText())

    @UnstableDefault
    override suspend fun action(clientStore: ClientStore, message: Message) {
        clientStore.channels[message.channelId].sendMessage("${message.author.mention}, ${data.random().run { first.random() + ' ' + second.random() }}")
    }
}