package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.TextChannelBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.*
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class Clear: LocalizedGuildCommand {
    override val name = "clear"

    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.ManageMessages
    override val cmdType = Command.CommandGroup.MODER
    override val requiredPermission: Permission? = Permission.ManageMessages

    @OptIn(ExperimentalTime::class)
    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        val limit = args.firstOrNull()?.toIntOrNull()
        if (limit == null || limit !in 1..100) {
            message.reply(texts.error())
            return
        }

        val messages = mutableListOf<Snowflake>()
        val timeLimit = message.id.timeStamp.minus(Duration.days(2))
        var lastMessage = message.id
        do {
            message.channel.getMessagesBefore(lastMessage)
                .onEach { lastMessage = it.id }
                .filter { it.id.timeStamp > timeLimit }
                .filter { message.mentionedUserIds.isEmpty() || it.author!!.id in message.mentionedUserIds }
                .take(limit - messages.size)
                .collect {
                    messages.add(it.id)
                }
        } while (messages.size < limit && lastMessage.timeStamp > timeLimit)

        if (messages.isEmpty()) {
            message.reply(texts["empty"])
            return
        }

        if (messages.size == 1)
            message.channel.deleteMessage(messages.first())
        else
            TextChannelBehavior(guild.id, message.channelId, message.kord).bulkDelete(messages.toSet())

        message.reply(texts["done"].format(messages.size))
    }
}