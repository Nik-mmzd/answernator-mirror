package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*
import kotlin.random.Random
import com.jessecorbett.diskord.dsl.message as dslmessage

private val random = Random(System.currentTimeMillis())
@UnstableDefault
class Dice: LocalizedCommand {
    override val name: String = "dice"
    private val diceLimit: Int
    private val throwsLimit: Int
    private val throwsSumLimit: Int
    private val triesLimit: Int

    init {
        val props = Properties()
        props.load(javaClass.classLoader.getResourceAsStream("properties/dice.properties"))
        diceLimit = props.getProperty("dice.diceLimit").toInt()
        throwsLimit = props.getProperty("dice.throwsLimit").toInt()
        throwsSumLimit = props.getProperty("dice.throwsSumLimit").toInt()
        triesLimit = props.getProperty("dice.triesLimit").toInt()
    }

    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        val sum = message.words.getOrNull(4) == "sum"
        val dice = message.words.getOrNull(1)?.toInt() ?: 6
        val throws = message.words.getOrNull(2)?.toInt() ?: 1
        val tries = message.words.getOrNull(3)?.toInt() ?: 1

        if (tries > triesLimit || tries < 1) return textMessage(formatString(locale, "triesLimit", triesLimit))
        if (!sum && throws > throwsLimit) return textMessage(formatString(locale, "throwsLimit", throwsLimit))
        if (throws > throwsSumLimit || throws < 1) return textMessage(formatString(locale, "throwsSumLimit", throwsSumLimit))
        if (dice > diceLimit || dice < 2) return textMessage(formatString(locale, "diceLimit", diceLimit))

        return dslmessage {
            title = getString(locale, "title")
            color = random.nextInt(0, 16777215)
            thumbnail = EmbedImage("https://files.mcmodder.ru/answernator/dice.jpg")
            repeat(tries) {
                if (sum) {
                    field(formatString(locale, "try", it), random.nextInt(1*throws, dice*throws).toString(), false)
                    return@repeat
                }

                val list: MutableList<Int> = mutableListOf()
                repeat(throws) {
                    list.add(random.nextInt(1, dice))
                }
                field(formatString(locale, "try", it), list.joinToString(" "), false)
            }
        }
    }
}