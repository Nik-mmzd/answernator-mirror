package pw.modder.answernator.tools.commands.localized

import dev.kord.common.Color
import dev.kord.core.entity.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.tools.diceHelper.*
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals.random
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import kotlin.math.sin

private val logger = KotlinLogging.logger {}
class Dice: LocalizedCommand {
    override val name: String = "dice"
    override val cmdType = Command.CommandGroup.FUN

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle, config: Config?) {
        val params = message.content.split(" ", limit = 2).getOrNull(1)?.takeIf { it.isNotEmpty() } ?: "1d6"

        val data = try {
            DiceParser(DiceTokenizer(params)).parse()
        } catch (e: InvalidArgumentException) {
            logger.error { e.printStackTrace() }
            message.reply(texts.error("error.format"))
            return
        } catch (e: DiceLimitExceededException) {
            message.reply(texts["error.limits"].format(texts["error.limits.${e.name}"], e.value, e.limit))
            return
        }

        message.replyEmbed {
            title = texts["title"]
            color = Color(random.nextInt(0, 16777215))
            thumbnail {
                url = "https://files.modder.pw/answernator/dice.jpg"
            }

            data.forEach { set ->
                field(texts["roll"].format(DiceSerializer(set.tokens).stringify()), false) {
                    set.roll().joinToString(separator = "\n") { singleRoll ->
                        singleRoll.joinToString(separator = " ", postfix = " (**${singleRoll.sum() + set.modifier}**)")
                    }
                }
            }
        }
    }
}