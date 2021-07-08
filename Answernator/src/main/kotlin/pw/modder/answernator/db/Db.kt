package pw.modder.answernator.db

import com.jessecorbett.diskord.api.model.Guild
import com.jessecorbett.diskord.api.rest.client.GuildClient
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.guild.*
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

object Db {
    private const val dbfile = "answernator"

    init {
        Database.connect("jdbc:h2:./$dbfile;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", user = "root", password = "")

        val newMutesExists = Mutes.exists()
        val newConfigsExists = Configs.exists()

        transaction {
            SchemaUtils.create (Configs, Mutes, MutesRef)
        }

        if (!newConfigsExists) {
            GuildConfig.all().forEach { guild ->
                val log = LogConfig.find { LogConfigs.guildId eq guild.guildId }.single()
                val texts = LocaleBundle("botGlobal", Locale(guild.lang))

                val newconf = Config.new {
                    guildId = guild.guildId
                    features = 0
                    lang = guild.lang
                    greeting = guild.greetingText
                    greetingChannel = guild.greetingsChannel.takeUnless { it.isEmpty() }
                    muteRole = guild.muteRole.takeUnless { it.isEmpty() }
                    defaultRole = guild.defaultRole.takeUnless { it.isEmpty() }
                    antiSpamWarn = 3
                    antiSpamBan = 5
                    antiSpamWarnText = texts.getString("bot.antispam.warning")
                    antiSpamBanText = texts.getString("bot.antispam.reason")
                    memberBanLogChannel = log.memberBanLogChannel.takeUnless { it.isEmpty() }
                    memberJoinLogChannel = log.memberJoinLogChannel.takeUnless { it.isEmpty() }
                    memberLeaveLogChannel = log.memberLeaveLogChannel.takeUnless { it.isEmpty() }
                    memberUnbanLogChannel = log.memberUnbanLogChannel.takeUnless { it.isEmpty() }
                    memberMuteLogChannel = log.memberMuteLogChannel.takeUnless { it.isEmpty() }
                    memberUnmuteLogChannel = log.memberUnmuteLogChannel.takeUnless { it.isEmpty() }
                }

                if (guild.antiSpam) newconf.enable(Features.ANTI_SPAM)
                if (guild.greetNewUsers) newconf.enable(Features.GREETING)
                if (guild.defaultRole.isNotEmpty()) newconf.enable(Features.DEFAULT_ROLE)

                if (log.memberBanLogChannel.isNotEmpty()) newconf.enable(Features.LOG_BAN)
                if (log.memberUnbanLogChannel.isNotEmpty()) newconf.enable(Features.LOG_UNBAN)
                if (log.memberMuteLogChannel.isNotEmpty()) newconf.enable(Features.LOG_MUTE)
                if (log.memberUnmuteLogChannel.isNotEmpty()) newconf.enable(Features.LOG_UNMUTE)
                if (log.memberJoinLogChannel.isNotEmpty()) newconf.enable(Features.LOG_JOIN)
                if (log.memberLeaveLogChannel.isNotEmpty()) newconf.enable(Features.LOG_LEAVE)

            }
        }

        if (!newMutesExists) {
            GuildMutes.selectAll().forEach {
                Mute.new {
                    guild = it[GuildMutes.guildId]
                    memberId = it[GuildMutes.memberId]
                }
            }
        }

        if (GuildConfigs.exists()) SchemaUtils.drop(GuildConfigs)
        if (LogConfigs.exists()) SchemaUtils.drop(LogConfigs)
        if (GuildMutes.exists()) SchemaUtils.drop(GuildMutes)
    }

    fun updateConfig(guildId: String, block: Config.() -> Unit) {
        transaction {
            Config.find { Configs.guildId eq guildId }.first().block()
        }
    }

    private fun isMuted(guildId: String, memberId: String): Boolean {
        return transaction {
            GuildMutes.select {
                GuildMutes.memberId eq memberId and(GuildMutes.guildId eq guildId)
            }.count()
        } > 0
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