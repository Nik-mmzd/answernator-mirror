package pw.modder.answernator.db

import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.guild.*
import pw.modder.answernator.db.guild.GuildConfig as NewGuildConfig
import pw.modder.answernator.db.guild.LogConfig as NewLogConfig
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

object Db {
    private val logger = KotlinLogging.logger {  }
    private const val dbfile = "answernator"

    fun initDb() {
        Database.connect("jdbc:h2:./$dbfile;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", user = "root", password = "")

        val newMutesExists = transaction { Mutes.exists() }
        val newConfigsExists = transaction { Configs.exists() }
        logger.info { "Newmutes: $newMutesExists newconfigs $newConfigsExists" }

        transaction {
            SchemaUtils.create (Configs, Mutes, MutesRef, BlacklistedCommands, BlacklistedCommandsRef)
        }

        if (!newConfigsExists) {
            logger.info { "Started newconfig transaction" }
            transaction {
                GuildConfig.all().forEach { guild ->
                    logger.info { "migrating ${guild.guildId}" }
                    val log = LogConfig.find { LogConfigs.guildId eq guild.guildId }.singleOrNull()
                    val texts = LocaleBundle("botGlobal", Locale(guild.lang))

                    val newconf = Config.new {
                        guildId = guild.guildId
                        features = 0
                        lang = guild.lang
                        cmdPrefix = Globals.config.prefix
                        greeting = guild.greetingText
                        greetingChannel = guild.greetingsChannel.takeUnless { it.isEmpty() }
                        muteRole = guild.muteRole.takeUnless { it.isEmpty() }
                        defaultRole = guild.defaultRole.takeUnless { it.isEmpty() }
                        antiSpamWarn = 3
                        antiSpamBan = 5
                        antiSpamWarnText = texts.getString("bot.antispam.warning")
                        antiSpamBanText = texts.getString("bot.antispam.reason")
                        memberBanLogChannel = log?.memberBanLogChannel?.takeUnless { it.isEmpty() }
                        memberJoinLogChannel = log?.memberJoinLogChannel?.takeUnless { it.isEmpty() }
                        memberLeaveLogChannel = log?.memberLeaveLogChannel?.takeUnless { it.isEmpty() }
                        memberUnbanLogChannel = log?.memberUnbanLogChannel?.takeUnless { it.isEmpty() }
                        memberMuteLogChannel = log?.memberMuteLogChannel?.takeUnless { it.isEmpty() }
                        memberUnmuteLogChannel = log?.memberUnmuteLogChannel?.takeUnless { it.isEmpty() }
                    }

                    if (guild.antiSpam) newconf.enable(Features.ANTI_SPAM)
                    if (guild.greetNewUsers) newconf.enable(Features.GREETING)
                    if (guild.defaultRole.isNotEmpty()) newconf.enable(Features.DEFAULT_ROLE)

                    if (log != null) {
                        if (log.memberBanLogChannel.isNotEmpty()) newconf.enable(Features.LOG_BAN)
                        if (log.memberUnbanLogChannel.isNotEmpty()) newconf.enable(Features.LOG_UNBAN)
                        if (log.memberMuteLogChannel.isNotEmpty()) newconf.enable(Features.LOG_MUTE)
                        if (log.memberUnmuteLogChannel.isNotEmpty()) newconf.enable(Features.LOG_UNMUTE)
                        if (log.memberJoinLogChannel.isNotEmpty()) newconf.enable(Features.LOG_JOIN)
                        if (log.memberLeaveLogChannel.isNotEmpty()) newconf.enable(Features.LOG_LEAVE)
                    }
                }
            }
        }

        if (!newMutesExists) {
            logger.info { "newmutes: transaction" }
            transaction {
                GuildMutes.selectAll().forEach {
                    logger.info { "migrating: ${it[GuildMutes.guildId]} ${it[GuildMutes.memberId]}" }
                    Mute.new {
                        guild = it[GuildMutes.guildId]
                        memberId = it[GuildMutes.memberId]
                    }
                }
            }

        }

        transaction {
            logger.info { "Dropping tables" }
            if (GuildConfigs.exists()) SchemaUtils.drop(GuildConfigs)
            if (LogConfigs.exists()) SchemaUtils.drop(LogConfigs)
            if (GuildMutes.exists()) SchemaUtils.drop(GuildMutes)
        }
    }

    fun updateConfig(guildId: String, block: Config.() -> Unit) {
        transaction {
            Config.find { Configs.guildId eq guildId }.first().block()
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
            Config.find { Configs.guildId eq guildId }.first()
        }
    }

    fun getGuildConfig(guildId: String): NewGuildConfig {
        return transaction {
            NewGuildConfig.find { Configs.guildId eq guildId }.first()
        }
    }

    fun getLogConfig(guildId: String): NewLogConfig {
        return transaction {
            NewLogConfig.find { Configs.guildId eq guildId }.first()
        }
    }

    fun getAntiSpamConfig(guildId: String): AntiSpamConfig {
        return transaction {
            AntiSpamConfig.find { Configs.guildId eq guildId }.first()
        }
    }
}