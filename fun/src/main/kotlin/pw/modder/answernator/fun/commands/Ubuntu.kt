package pw.modder.answernator.`fun`.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.mention
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import pw.modder.answernator.`fun`.utils.UbuntuWord
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*

@UnstableDefault
class Ubuntu: LocalizedCommand {
    override val name: String = "ubuntu"
    override val cmdType = Command.CommandGroup.FUN
    private val data = Json(JsonConfiguration.Default)
        .parse(UbuntuWord.serializer().list, javaClass.classLoader.getResourceAsStream("ubuntu.json").reader().readText())

    @UnstableDefault
    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        return textMessage("${message.author.mention}, ${data.random().run { first.random() + ' ' + second.random() }}")
    }
}