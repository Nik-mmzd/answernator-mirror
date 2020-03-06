package pw.modder.answernator.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.jessecorbett.diskord.api.model.Role
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import mu.KotlinLogging
import java.util.concurrent.TimeUnit


object RolesCache {
    private val logger = KotlinLogging.logger {}
    private val cache: Cache<String, List<Role>> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterAccess(1, TimeUnit.DAYS)
        .build()

    suspend fun GuildClient.getRolesCached(): List<Role> {
        return cache.getIfPresent(guildId) ?: run {
            val roles = getRoles()
            cache.put(guildId, roles)
            logger.debug { "got member roles for guild $guildId" }
            return roles
        }
    }

    @DiskordDsl
    fun Bot.enableRolesCache() {
        roleDeleted { roleUpdate ->
            cache.getIfPresent(roleUpdate.guildId)?.run {
                cache.put(roleUpdate.guildId, this.dropWhile { it.id == roleUpdate.roleId })
                logger.debug { "Dropped role ${roleUpdate.roleId} at ${roleUpdate.guildId}" }
            }
        }
        roleUpdated { roleUpdate ->
            cache.getIfPresent(roleUpdate.guildId)?.run {
                cache.put(roleUpdate.guildId, this.dropWhile { it.id == roleUpdate.role.id } + roleUpdate.role)
                logger.debug { "Dropped role ${roleUpdate.role.id} at ${roleUpdate.guildId}" }
            }
        }
    }
}