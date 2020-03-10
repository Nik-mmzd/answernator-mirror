package pw.modder.answernator.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.jessecorbett.diskord.api.model.Guild
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
object GuildCache {
    private val cache: Cache<String, Guild> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterAccess(1, TimeUnit.DAYS)
        .build()

    suspend fun GuildClient.getCached(): Guild {
        return cache.getIfPresent(guildId) ?: cacheGuild()
    }

    private suspend fun GuildClient.cacheGuild(): Guild {
        val guild = get()
        cache.put(guildId, guild)
        logger.debug { "Cached ${guild.name} (ID $guildId)" }
        return guild
    }

    @DiskordDsl
    fun Bot.enableGuildCache() {
        guildCreated {
            cache.put(it.id, Guild(
                id = it.id,
                name = it.name,
                iconHash = it.icon,
                splashHash = null,
                userIsOwner = it.userIsOwner,
                ownerId = it.ownerId,
                permissions = it.permissions,
                region = it.region,
                afkChannelId = it.afkChannelId,
                afkTimeoutSeconds = it.afkTimeoutSeconds,
                embedEnabled = it.embedEnabled,
                embeddedChannelId = it.embeddedChannelId,
                verificationLevel = it.verificationLevel,
                defaultMessageNotificationLevel = it.defaultMessageNotificationLevel,
                explicitContentFilterLevel = it.explicitContentFilterLevel,
                roles = it.roles,
                emojis = it.emojis,
                features = it.features,
                mfaLevel = it.mfaLevel,
                owningApplicationId = it.owningApplicationId,
                widgetEnabled = it.widgetEnabled,
                widgetChannelId = it.widgetChannelId,
                systemMessageChannelId = it.systemMessageChannelId
            ))
            logger.debug { "Cached ${it.name} (ID ${it.id}). Reason: Guild created" }
        }
        guildDeleted {
            logger.debug { "Invalidated ${it.name} (ID ${it.id}). Reason: Guild deleted" }
            cache.invalidate(it.id)
        }
        guildUpdated {
            logger.debug { "Re-cached ${it.name} (ID ${it.id}). Reason: Guild updated" }
            cache.put(it.id, it)
        }
        roleDeleted { deletedRole ->
            val guild = cache.getIfPresent(deletedRole.guildId) ?: return@roleDeleted
            val roles = guild.roles.filterNot { it.id == deletedRole.roleId }
            logger.debug { "Re-cached guild ${guild.name} (ID: ${guild.id}). Reason: Role ${deletedRole.roleId} deleted" }
            cache.put(deletedRole.guildId, guild.copy(roles = roles))
        }
        roleUpdated { updatedRole ->
            val guild = cache.getIfPresent(updatedRole.guildId) ?: return@roleUpdated
            val roles = guild.roles.filterNot { it.id == updatedRole.role.id } + updatedRole.role
            logger.debug { "Re-cached guild ${guild.name} (ID: ${guild.id}). Reason: Role ${updatedRole.role.name} (ID ${updatedRole.role.id}) updated" }
            cache.put(updatedRole.guildId, guild.copy(roles = roles))
        }
    }
}