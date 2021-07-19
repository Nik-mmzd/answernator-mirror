package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.kord.*
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class SnowFlake: LocalizedCommand {
    override val name = "snowflake"
    override val cmdType = Command.CommandGroup.OTHER

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle, config: Config?) {
        if (args.isEmpty()) {
            message.reply(texts.getErrorString())
            return
        }

        message.reply {
            embed {
                title = texts.getString("title")
                description = texts.getString("description")

                args.take(20).map { it.takeIf { it.length == 18 }?.toLongOrNull() }.map { it?.run { Snowflake(this) } }.forEach {
                    if (it == null) {
                        field(texts.getString("field.invalid"), false) { texts.getString("field.invalid.text") }
                        return@forEach
                    }

                    field(it.asString, false) { texts.formatString("field.text", it.timestamp, it.worker, it.process, it.increment) }
                }
            }
            allowedMentions { repliedUser = false }
        }
    }
}