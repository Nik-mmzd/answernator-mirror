package pw.modder.answernator.`fun`.commands

import dev.kord.core.entity.Message
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pw.modder.answernator.`fun`.utils.UbuntuWord
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Ubuntu: LocalizedCommand {
    override val name: String = "ubuntu"
    override val cmdType = Command.CommandGroup.FUN
    private val data = Json.decodeFromString(ListSerializer(UbuntuWord.serializer()), javaClass.classLoader.getResourceAsStream("ubuntu.json").reader().readText())

    private fun randomReply() = data.random().let { "${it.first.random()} ${it.second.random()}" }

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle, config: Config?) {
        message.reply(randomReply())
    }
}