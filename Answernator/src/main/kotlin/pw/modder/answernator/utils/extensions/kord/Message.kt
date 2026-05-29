package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.allowedMentions
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals

@Deprecated("Switch to v4")
val Message.words: List<String> get() {
    return content.split(' ')
}

@Deprecated("Switch to v4")
val Message.guildId: Snowflake? get() {
    return data.guildId.value
}

@Deprecated("Switch to v4")
val Message.channelType: Command.ChannelTypes get() {
    if (data.guildId.value == null) return Command.ChannelTypes.DIRECT
    return Command.ChannelTypes.GUILD
}

@Deprecated("Switch to v4")
val Message.authorId: Snowflake get() = data.author.id

@Deprecated("Switch to v4")
suspend fun Message.reply(content: String) = channel.createMessage {
    this.content = content
    noReplyMention()
    messageReference = data.referencedMessage.value?.id ?: id
}

@Deprecated("Switch to v4")
suspend inline fun Message.replyEmbed(block: EmbedBuilder.() -> Unit) = channel.createMessage {
    embed(block)
    noReplyMention()
    messageReference = referencedMessage?.id ?: id
}

@Deprecated("Switch to v4")
fun Message.isFromBotAuthor(): Boolean {
    return data.author.id.toString() == Globals.config.author
}

@Deprecated("Switch to v4")
fun MessageCreateBuilder.noReplyMention() {
    allowedMentions {
        repliedUser = false
    }
}
