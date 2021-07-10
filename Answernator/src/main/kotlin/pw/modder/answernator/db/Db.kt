package pw.modder.answernator.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.guild.*
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

object Db {
    private const val dbfile = "answernator"

    fun initDb() {
        Database.connect("jdbc:h2:./$dbfile;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", user = "root", password = "")

        transaction {
            SchemaUtils.create (Configs, Mutes, BlacklistedCommands)
        }
    }

    fun createDefaultConfig(guild: String): Config {
        val conf = Globals.config
        val texts = LocaleBundle("botGlobal", Locale(conf.lang))

        return transaction {
            Config.new {
                guildId = guild
                features = 0
                lang = Globals.config.lang
                cmdPrefix = Globals.config.prefix
                greeting = texts.getString("bot.greeting.message")
                greetingChannel = null
                muteRole = null
                defaultRole = null
                antiSpamWarn = 3
                antiSpamBan = 5
                antiSpamWarnText = texts.getString("bot.antispam.warning")
                antiSpamBanText = texts.getString("bot.antispam.reason")
                memberBanLogChannel = null
                memberJoinLogChannel = null
                memberLeaveLogChannel = null
                memberUnbanLogChannel = null
                memberMuteLogChannel = null
                memberUnmuteLogChannel = null
            }
        }
    }

    fun getConfig(guildId: String): Config {
        return transaction {
            Config.find { Configs.guildId eq guildId }.firstOrNull()
        } ?: createDefaultConfig(guildId)
    }

    fun getGuildConfig(guildId: String): GuildConfig {
        return transaction {
            GuildConfig.find { Configs.guildId eq guildId }.firstOrNull()
        } ?: run {
            createDefaultConfig(guildId)
            getGuildConfig(guildId)
        }
    }

    fun getLogConfig(guildId: String): LogConfig {
        return transaction {
            LogConfig.find { Configs.guildId eq guildId }.firstOrNull()
        } ?: run {
            createDefaultConfig(guildId)
            getLogConfig(guildId)
        }
    }

    fun getAntiSpamConfig(guildId: String): AntiSpamConfig {
        return transaction {
            AntiSpamConfig.find { Configs.guildId eq guildId }.firstOrNull()
        } ?: run {
            createDefaultConfig(guildId)
            getAntiSpamConfig(guildId)
        }
    }

    fun isMuted(guildId: String, memberId: String): Boolean {
        return transaction { Mute.find { Mutes.guildId eq guildId and(Mutes.memberId eq memberId) }.count() > 0L }
    }

    fun getMute(guildId: String, memberId: String): Mute? {
        return transaction { Mute.find { Mutes.guildId eq guildId and(Mutes.memberId eq memberId) }.firstOrNull() }
    }

    fun isBlackListed(guildId: String, command: String): Boolean {
        return transaction { BlacklistedCommand.find { BlacklistedCommands.guildId eq guildId and(BlacklistedCommands.command eq command) }.count() > 0L }
    }

    fun getBlackListed(guildId: String): List<String> {
        return transaction { BlacklistedCommand.find { BlacklistedCommands.guildId eq guildId }.map { it.command } }
    }

    fun getBlackListedCommand(guildId: String, command: String): BlacklistedCommand? {
        return transaction { BlacklistedCommand.find { BlacklistedCommands.guildId eq guildId and(BlacklistedCommands.command eq command) }.firstOrNull() }
    }
}