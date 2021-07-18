package pw.modder.answernator.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import pw.modder.answernator.utils.extensions.kord.guildId
import java.util.concurrent.TimeUnit

object AntiSpamCache {
    private val cache: Cache<Triple<Snowflake, Snowflake, Int>, Int> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .build()

    private val messages: Cache<Pair<Snowflake, Snowflake>, Message> = CacheBuilder.newBuilder()
        .maximumSize(128)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build()

    fun putMessage(message: Message) {
        messages.put(message.guildId!! to message.author!!.id, message)
    }

    fun getMessage(guild: Snowflake, member: Snowflake): Message? {
        return messages.getIfPresent(guild to member)
            ?.also { messages.invalidate(guild to member) }
    }

    private fun hashCode(message: Message): Int {
        var result = message.content.hashCode()
        message.attachments.forEach {
            result = 31 * result + it.size.hashCode()
        }
        return result
    }

    fun increment(message: Message): Int {
        val triple = Triple(message.guildId!!, message.author!!.id, hashCode(message))
        val count = (cache.getIfPresent(triple) ?: 0)+1
        cache.put(triple, count)
        return count
    }
}