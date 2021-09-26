package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals

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

val Message.authorId: Snowflake get() = data.author.id

suspend fun Message.reply(content: String) = reply {
    this.content = content
    noReplyMention()
}

suspend inline fun Message.replyEmbed(block: EmbedBuilder.() -> Unit) = reply {
    embed(block)
    noReplyMention()
}

fun Message.isFromBotAuthor(): Boolean {
    return data.author.id.asString == Globals.config.author
}

fun MessageCreateBuilder.noReplyMention() {
    allowedMentions { repliedUser = false }
}