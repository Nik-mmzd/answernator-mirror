package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.rest.BulkMessageDelete
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*

@UnstableDefault
class Clear: LocalizedCommand {
    override val name = "clear"

    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_MESSAGES
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)

    val Message.sentAtInstant: Instant
        get() = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(sentAt))

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val channel = bot.clientStore.channels[message.channelId]
        val messages = mutableListOf<String>()
        val limit = message.words.getOrNull(1)?.run {
            toIntOrNull() ?: return texts.errorMessage()
        } ?: 100

        if (limit > 100) return texts.errorMessage()
        val mentionedUserIds = message.usersMentioned.map { it.id }
        val minusTwoWeeks = message.sentAtInstant.minus(2, ChronoUnit.WEEKS)

        var lastMessage = message
        do {
            messages.addAll(
                channel.getMessagesBefore(limit = limit, messageId = lastMessage.id)
                    .also { lastMessage = it.last() }
                    .filter { mentionedUserIds.isEmpty() || it.authorId in mentionedUserIds }
                    .take(limit - messages.size)
                    .filter { it.sentAtInstant.isAfter(minusTwoWeeks) }
                    .map { it.id }
            )
        } while (messages.size < limit && lastMessage.sentAtInstant.isAfter(minusTwoWeeks))

        channel.bulkDeleteMessages(BulkMessageDelete(
            messages.take(limit).toList()
        ))

        return texts.message("done", messages.size)
    }
}