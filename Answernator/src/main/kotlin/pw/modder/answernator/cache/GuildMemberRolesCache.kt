package pw.modder.answernator.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.dsl.Bot
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

object GuildMemberRolesCache {
    private val logger = KotlinLogging.logger {}
    private val cache: Cache<Pair<String, String>, List<String>> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterAccess(1, TimeUnit.DAYS)
        .build()

    suspend fun GuildClient.getMemberRolesCached(id: String): List<String> {
        return cache.getIfPresent(guildId to id) ?: run {
            val member = getMember(id)
            cache.put(guildId to id, member.roleIds)
            logger.debug { "got member ID: $id at guild $guildId" }
            return member.roleIds
        }
    }

    @DiskordDsl
    fun Bot.enableGuildMemberRolesCache() {
        userLeftGuild {
            cache.invalidate(it.guildId to it.user.id)
            logger.debug { "invalidated member ${it.user.username} (ID: ${it.user.id}) at guild ${it.guildId}" }
        }

        guildMemberUpdated {
            cache.getIfPresent(it.guildId to it.user.id)?.run {
                logger.debug { "updated member ${it.user.username} (ID: ${it.user.id}) at guild ${it.guildId}" }
                cache.put(it.guildId to it.user.id, it.roles)
            }
        }
    }
}

