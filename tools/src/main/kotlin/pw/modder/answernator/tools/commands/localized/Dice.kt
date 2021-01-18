package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import mu.KLogger
import mu.KotlinLogging
import pw.modder.answernator.tools.diceHelper.*
import pw.modder.answernator.tools.diceHelper.Dice
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals.random
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

private val logger = KotlinLogging.logger {}
class Dice: LocalizedCommand {
    override val name: String = "dice"
    override val cmdType = Command.CommandGroup.FUN

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val params = message.content.split(" ", limit = 2).getOrNull(1)?.takeIf { it.isNotEmpty() } ?: "1d6"

        val data = try {
            DiceParser(DiceTokenizer(params)).parse()
        } catch (e: InvalidArgumentException) {
            logger.error { e.printStackTrace() }
            return texts.errorMessage("error.format")
        } catch (e: DiceLimitExceededException) {
            return texts.message("error.limits", texts.getStringOrKey("error.limits.${e.name}"), e.value, e.limit)
        }

        return dslmessage {
            title = texts.getStringOrKey("title")
            color = random.nextInt(0, 16777215)
            thumbnail = EmbedImage("https://files.mcmodder.ru/answernator/dice.jpg")

            data.forEach { set ->
                field(texts.formatString("roll", DiceSerializer(set.tokens).stringify()), set.roll().joinToString(separator = "\n") { singleRoll ->
                    singleRoll.joinToString(separator = " ", postfix = " (**${singleRoll.sum()}**)")
                }, false)
            }
        }
    }
}