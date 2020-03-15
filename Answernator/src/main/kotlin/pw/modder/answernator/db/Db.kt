package pw.modder.answernator.db

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.jessecorbett.diskord.api.model.Guild
import com.jessecorbett.diskord.api.rest.client.GuildClient
import kotlinx.serialization.UnstableDefault
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit

@UnstableDefault
object Db {
    private const val guildConfigCacheSize = 32L
    private const val guildLogsConfigCacheSize = 32L
    private const val dbfile = "answernator"

    init {
        Database.connect("jdbc:h2:./$dbfile", driver = "org.h2.Driver", user = "root", password = "")

        transaction {
            SchemaUtils.create (GuildConfigs, LogConfigs)
        }
    }

    val guilds = CacheBuilder.newBuilder()
        .maximumSize(guildConfigCacheSize)
        .expireAfterAccess(1, TimeUnit.DAYS)
        .build(
            object : CacheLoader<String, GuildConfig>() {
                override fun load(key: String): GuildConfig {
                    return transaction {
                        GuildConfig.find { GuildConfigs.guildId eq key }.firstOrNull()
                            ?: GuildConfig.new {
                                guildId = key
                            }
                    }
                }
            }
        )

    val logs = CacheBuilder.newBuilder()
        .maximumSize(guildLogsConfigCacheSize)
        .expireAfterAccess(1, TimeUnit.DAYS)
        .build(
            object : CacheLoader<String, LogConfig>() {
                override fun load(key: String): LogConfig {
                    return transaction {
                        LogConfig.find { LogConfigs.guildId eq key }.firstOrNull()
                            ?: LogConfig.new {
                                guildId = key
                            }
                    }
                }
            }
        )

    fun updateGuildConfig(guildId: String, block: GuildConfigs.(UpdateStatement) -> Unit) {
        transaction {
            GuildConfigs.update({GuildConfigs.guildId eq guildId}, body = block)
        }
        guilds.invalidate(guildId)
    }

    fun updateLogConfig(guildId: String, block: LogConfigs.(UpdateStatement) -> Unit) {
        transaction {
            LogConfigs.update({LogConfigs.guildId eq guildId}, body = block)
        }
        logs.invalidate(guildId)
    }

    private fun isMuted(guildId: String, memberId: String): Boolean {
        return transaction {
            GuildMutes.select {
                GuildMutes.memberId eq memberId
                GuildMutes.guildId eq guildId
            }
        }.count() > 0
    }

    private fun mute(guild: String, member: String) {
        transaction {
            GuildMutes.insert {
                it[guildId] = guild
                it[memberId] = member
            }
        }
    }

    private fun unmute(guild: String, member: String) {
        transaction {
            GuildMutes.deleteWhere {
                GuildMutes.guildId eq guild
                GuildMutes.memberId eq member
            }
        }
    }

    fun GuildClient.memberIsMuted(memberId: String): Boolean {
        return isMuted(guildId, memberId)
    }

    fun Guild.memberIsMuted(memberId: String): Boolean {
        return isMuted(id, memberId)
    }

    fun GuildClient.muteMember(memberId: String) {
        mute(guildId, memberId)
    }

    fun Guild.muteMember(memberId: String) {
        mute(id, memberId)
    }

    fun GuildClient.unmuteMember(memberId: String) {
        unmute(guildId, memberId)
    }

    fun Guild.unmuteMember(memberId: String) {
        unmute(id, memberId)
    }
}