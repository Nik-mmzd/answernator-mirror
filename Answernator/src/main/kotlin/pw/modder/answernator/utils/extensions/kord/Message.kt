package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
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

suspend fun Message.reply(content: String) = reply { this.content = content }