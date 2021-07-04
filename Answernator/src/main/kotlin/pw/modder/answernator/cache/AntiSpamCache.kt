package pw.modder.answernator.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.util.authorId
import java.util.concurrent.TimeUnit

object AntiSpamCache {
    private val cache: Cache<Triple<String, String, Int>, Int> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .build()

    private fun hashCode(message: Message): Int {
        var result = message.content.hashCode()
        result = 31 * result + message.attachments.hashCode()
        return result
    }

    fun increment(message: Message): Int {
        val count = (cache.getIfPresent(Triple(message.guildId!!, message.authorId, hashCode(message))) ?: 0)+1
        cache.put(Triple(message.guildId!!, message.authorId, hashCode(message)), count)
        return count
    }
}