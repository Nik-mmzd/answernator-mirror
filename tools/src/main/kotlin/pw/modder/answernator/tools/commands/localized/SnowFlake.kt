package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.tools.utils.Snowflake
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import com.jessecorbett.diskord.dsl.message as dslmessage

class SnowFlake: LocalizedCommand {
    override val name = "snowflake"
    override val cmdType = Command.CommandGroup.OTHER

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        if (message.words.size < 2) return texts.getErrorString().toMessage()

        return dslmessage {
            title = texts.getString("title")
            description = texts.getString("description")

            message.words.drop(1).take(20).map { it.takeIf { it.length == 18 }?.toLongOrNull() }.map { it?.run { Snowflake(this) } }.forEach {
                if (it == null) {
                    field(texts.getString("field.invalid"), texts.getString("field.invalid.text"), false)
                    return@forEach
                }

                field(it.snowflake.toString(), texts.formatString("field.text", it.timestamp, it.worker, it.process, it.increment), false)
            }
        }
    }
}