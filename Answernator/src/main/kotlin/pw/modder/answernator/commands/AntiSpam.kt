package pw.modder.answernator.commands

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class AntiSpam: LocalizedGuildCommand {
    override val name = "antispam"

    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle) {
        val config = Db.getAntiSpamConfig(guild.id)

        if (args.isEmpty()) {
            message.reply { content = texts.getErrorString() }
            return
        }

        when(args[0].toLowerCase()) {
            "get", "show" -> message.reply {
                embed {
                    title = texts.getString("title")
                    description = texts.formatString("description",
                        texts.getString("enabled.${config.isEnabled(Features.ANTI_SPAM)}"),
                        texts.getString("enabled.${config.isEnabled(Features.ANTI_SPAM_SILENT)}"),
                        config.antiSpamWarn,
                        config.antiSpamBan
                    )

                    field(texts.getString("warn.title"), false) { config.antiSpamWarnText.replace("%1\$s", "%user%") }
                    field(texts.getString("ban.title"), false) { config.antiSpamBanText }
                }
                allowedMentions { repliedUser = false }
            }
            "enable" -> {
                transaction { config.enable(Features.ANTI_SPAM) }
                message.reply(texts.getString("enabled"))
            }
            "disable" -> {
                transaction { config.disable(Features.ANTI_SPAM) }
                message.reply(texts.getString("disabled"))
            }
            "silent" -> {
                when(args.getOrNull(1)) {
                    "enable" -> {
                        transaction { config.enable(Features.ANTI_SPAM_SILENT) }
                        message.reply(texts.getString("enabled.silent"))
                    }
                    "disable" -> {
                        transaction { config.disable(Features.ANTI_SPAM_SILENT) }
                        message.reply(texts.getString("disabled.silent"))
                    }
                    else -> message.reply(texts.getErrorString())
                }
            }
            "warning", "warn" -> {
                val warn = args.drop(1).joinToString(separator = " ").replace("%user%", "%1\$s")
                if (warn.isEmpty()) {
                    message.reply(texts.getString("warn.empty"))
                    return
                }
                transaction { config.antiSpamWarnText = warn }
                message.reply(texts.getString("warn.set"))
            }
            "reason", "ban" -> {
                val ban = args.drop(1).joinToString(separator = " ")
                if (ban.isEmpty()) {
                    message.reply(texts.getString("ban.empty"))
                    return
                }
                transaction { config.antiSpamBanText = ban }
                message.reply(texts.getString("ban.set"))
            }
            "limit" -> {
                when(args.getOrNull(1)) {
                    "ban" -> {
                        val limit = args.getOrNull(2)?.toInt()?.takeIf { it > 1 }
                        if (limit == null) {
                            message.reply(texts.getString("limit.invalid"))
                            return
                        }
                        transaction { config.antiSpamBan = limit }
                        message.reply(texts.formatString("limit.ban", limit))
                    }
                    "warn", "warning" -> {
                        val limit = args.getOrNull(2)?.toInt()?.takeIf { it > 1 }
                        if (limit == null) {
                            message.reply(texts.getString("limit.invalid"))
                            return
                        }
                        transaction { config.antiSpamWarn = limit }
                        message.reply(texts.formatString("limit.warn", limit))
                    }
                    else -> message.reply(texts.getErrorString())
                }
            }
            else -> message.reply(texts.getErrorString())
        }
    }
}