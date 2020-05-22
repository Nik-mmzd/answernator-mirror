package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.rest.BulkMessageDelete
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class Clear: LocalizedCommand {
    override val name = "clear"

    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_MESSAGES
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val cmdType = Command.CommandGroup.MODER
    override val requiredPermission: Permission? = Permission.MANAGE_MESSAGES

    val Message.sentAtDate
        get() = OffsetDateTime.parse(sentAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val channel = bot.clientStore.channels[message.channelId]
        val messages = mutableListOf<String>()
        val limit = message.words.getOrNull(1)?.toIntOrNull() ?: return texts.errorMessage()

        if (limit > 100 || limit < 1) return texts.errorMessage()
        val mentionedUserIds = message.usersMentioned.map { it.id }

        val timeLimit = message.sentAtDate.minus(2, ChronoUnit.DAYS)
        val msgLimit = if (mentionedUserIds.isEmpty()) limit else 100

        var lastMessage = message
        do {
            channel.getMessagesBefore(limit = msgLimit, messageId = lastMessage.id)
                .also { lastMessage = it.last() }
                .filter { mentionedUserIds.isEmpty() || it.authorId in mentionedUserIds }
                .take(limit - messages.size)
                .filter { it.sentAtDate.isAfter(timeLimit) }
                .map { it.id }.takeIf { it.isNotEmpty() }?.run { messages.addAll(this) }

        } while (messages.size < limit && lastMessage.sentAtDate.isAfter(timeLimit))

        if (messages.isEmpty()) return texts.message("empty")

        if (messages.size == 1) {
            channel.deleteMessage(messages.single())
        } else {
            channel.bulkDeleteMessages(BulkMessageDelete(
                messages.toList()
            ))
        }

        return texts.message("done", messages.size)
    }
}