package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageCreateBuilder
import pw.modder.answernator.utils.Command

val Message.words: List<String> get() {
    return content.split(' ')
}

val Message.guildId: Snowflake? get() {
    return data.guildId.value
}

val Message.channelType: Command.ChannelTypes get() {
    if (data.guildId.value == null) return Command.ChannelTypes.DIRECT
    return Command.ChannelTypes.GUILD
}

val Message.authorId: String get() = data.author.id.asString

fun MessageCreateBuilder.noReplyMention() {
    allowedMentions { repliedUser = false }
}

suspend fun Message.reply(content: String) = reply {
    this.content = content
    noReplyMention()
}

suspend fun Message.replyEmbed(block: EmbedBuilder.() -> Unit) = reply {
    embed(block)
    noReplyMention()
}