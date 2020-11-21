package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.tools.diceHelper.DiceConfig
import pw.modder.answernator.tools.diceHelper.DiceLimitExceededException
import pw.modder.answernator.tools.diceHelper.DiceSet
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals.random
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

class Dice: LocalizedCommand {
    override val name: String = "dice"
    override val cmdType = Command.CommandGroup.FUN

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val words = message.words.drop(1)

        if (words.count() > 10) return texts.message("error.limits", DiceConfig.dicesLimit, DiceConfig.dotsLimit, DiceConfig.modLimit, DiceConfig.triesLimit, DiceConfig.throwsLimit)

        val config =  try {
            words.map { DiceSet.parse(it) }
                .ifEmpty { listOf(DiceSet(DiceConfig.defautDicePips, DiceConfig.defaultDices, DiceConfig.defaultModifier, DiceConfig.defaultTries)) }
        } catch (_: NumberFormatException) {
            return texts.errorMessage("error.format")
        } catch (_: DiceLimitExceededException) {
            return texts.message("error.limits", DiceConfig.dicesLimit, DiceConfig.dotsLimit, DiceConfig.modLimit, DiceConfig.triesLimit, DiceConfig.throwsLimit)
        }

        return dslmessage {
            title = texts.getStringOrKey("title")
            color = random.nextInt(0, 16777215)
            thumbnail = EmbedImage("https://files.mcmodder.ru/answernator/dice.jpg")

            config.forEach { set ->
                field(texts.formatString("roll", set.count, set.max, set.modifier), set.roll().joinToString(separator = "\n") { roll ->
                    roll.joinToString(separator = " ", postfix = " (**${roll.sum()}**)")
                }, false)
            }
        }
    }
}