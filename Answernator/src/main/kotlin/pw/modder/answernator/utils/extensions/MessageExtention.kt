package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.Message

private val channelMentionRegex = Regex("<#(\\d{18})>")
val Message.channelsIdsMentioned: List<String> get() {
    return channelMentionRegex.findAll(content).map { it.groupValues.last() }.toList()
}