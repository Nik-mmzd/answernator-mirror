package pw.modder.answernator.`fun`.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.mention
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import pw.modder.answernator.`fun`.utils.UbuntuWord
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Ubuntu: LocalizedCommand {
    override val name: String = "ubuntu"
    override val cmdType = Command.CommandGroup.FUN
    private val data = Json(JsonConfiguration.Stable)
        .parse(UbuntuWord.serializer().list, javaClass.classLoader.getResourceAsStream("ubuntu.json").reader().readText())

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        return textMessage("${message.author.mention}, ${data.random().run { first.random() + ' ' + second.random() }}")
    }
}