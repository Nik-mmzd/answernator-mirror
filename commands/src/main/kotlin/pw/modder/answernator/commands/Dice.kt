package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import kotlin.random.Random

@UnstableDefault
class Dice: Command {
    override val name: String = "dice"

    override suspend fun action(clientStore: ClientStore, message: Message) {
        val sum = message.words.getOrNull(4) == "sum"
        val dice = message.words.getOrNull(1)?.toInt() ?: 6
        val throws = message.words.getOrNull(2)?.toInt() ?: 1
        val tries = message.words.getOrNull(3)?.toInt() ?: 1

        if (tries > 20 || tries < 1) error("Tries count limited to 20")
        if (!sum && throws > 20) error("Throws count limited to 20 without summing")
        if (throws > 1000 || throws < 1) error("Throws count limited to 1000")
        if (dice > 64 || dice < 2) error("Dice must be 2 to 64")

        val msg = com.jessecorbett.diskord.dsl.message {
            title = "Dice"
            color = Random.nextInt(0, 16777215)
            thumbnail = EmbedImage("https://files.mcmodder.ru/answernator/dice.jpg")
            repeat(tries) {
                if (sum) {
                    field("Try $it", Random.nextInt(1*throws, dice*throws).toString(), false)
                    return@repeat
                }

                val list: MutableList<Int> = mutableListOf()
                repeat(throws) {
                    list.add(Random.nextInt(1, dice))
                }
                field("Try $it", list.joinToString(" "), false)
            }
        }
        clientStore.channels[message.channelId].sendMessage(msg.text, msg.embed())
    }

}