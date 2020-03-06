package pw.modder.answernator.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

object GuildOwnerCache {
    private val logger = KotlinLogging.logger {}
    private val cache: Cache<String, String> = CacheBuilder.newBuilder()
        .maximumSize(128)
        .expireAfterAccess(1, TimeUnit.DAYS)
        .build()

    suspend fun GuildClient.getOwnerCached(): String {
        return cache.getIfPresent(guildId) ?: run {
            val guild = get()
            cache.put(guildId, guild.ownerId)
            logger.debug { "got owner ID: $${guild.ownerId} for guild $guildId" }
            return guild.ownerId
        }
    }

    @DiskordDsl
    fun Bot.enableGuildOwnerCache() {
        guildCreated {
            cache.put(it.id, it.ownerId)
        }
        guildDeleted {
            cache.invalidate(it.id)
        }
        guildUpdated {
            cache.put(it.id, it.ownerId)
        }
    }
}