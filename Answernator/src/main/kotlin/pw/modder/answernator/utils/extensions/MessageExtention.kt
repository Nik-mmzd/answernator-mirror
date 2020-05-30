package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.Message
import pw.modder.answernator.utils.Command

private val channelMentionRegex = Regex("<#(\\d{18})>")
val Message.channelsIdsMentioned: List<String>
    get() = channelMentionRegex.findAll(content).map { it.groupValues.last() }.toList()

val Message.channelType: Command.ChannelTypes
    get() = if (guildId == null) Command.ChannelTypes.DIRECT else Command.ChannelTypes.GUILD