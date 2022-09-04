package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Snowflake
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
            message.reply(texts.error())
            return
        }

        message.replyEmbed {
            title = texts["title"]
            description = texts["description"]

            args.take(20).map { it.takeIf { it.length == 18 }?.toLongOrNull() }.map { it?.run { Snowflake(this) } }.forEach {
                if (it == null) {
                    field(texts["field.invalid"], false) { texts["field.invalid.text"] }
                    return@forEach
                }

                field(it.toString(), false) {
                    texts["field.text"].format(
                        it.timestamp,
                        it.workerId,
                        it.processId,
                        it.increment,
                        it.timestampMention
                    )
                }
            }
        }
    }
}