package pw.modder.answernator.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.kord.core.entity.Message
import pw.modder.answernator.utils.extensions.kord.authorId
import pw.modder.answernator.utils.extensions.kord.guildId
import java.util.concurrent.TimeUnit

object AntiSpamCache {
    private val cache: Cache<Triple<String, String, Int>, Int> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .build()

    private fun hashCode(message: Message): Int {
        var result = message.content.hashCode()
        message.attachments.forEach {
            result = 31 * result + it.size.hashCode()
        }
        return result
    }

    fun increment(message: Message): Int {
        val triple = Triple(message.guildId!!.asString, message.authorId, hashCode(message))
        val count = (cache.getIfPresent(triple) ?: 0)+1
        cache.put(triple, count)
        return count
    }
}