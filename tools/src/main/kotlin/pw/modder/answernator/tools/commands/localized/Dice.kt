package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import pw.modder.answernator.tools.diceHelper.*
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals.random
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

class Dice: LocalizedCommand {
    override val name: String = "dice"
    override val cmdType = Command.CommandGroup.FUN

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val params = message.content.split(" ", limit = 2).getOrNull(1) ?: ""

        val data = try {
            DiceParser(DiceTokenizer(params)).parse()
        } catch (_: InvalidArgumentException) {
            return texts.errorMessage("error.format")
        } catch (e: DiceLimitExceededException) {
            return texts.message("error.limits", texts.getStringOrKey("error.limits.${e.name}"), e.value, e.limit)
        }

        return dslmessage {
            title = texts.getStringOrKey("title")
            color = random.nextInt(0, 16777215)
            thumbnail = EmbedImage("https://files.mcmodder.ru/answernator/dice.jpg")

            data.forEach { set ->
                val roll = set.roll()
                val name = DiceSerializer(set.tokens).stringify()
                field(texts.formatString("roll", name), set.roll().joinToString(separator = "\n") { singleRoll ->
                    roll.joinToString(separator = " ", postfix = " (**${singleRoll.sum()}**)")
                }, false)
            }
        }
    }
}