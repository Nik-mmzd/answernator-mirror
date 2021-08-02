package pw.modder.answernator.commands

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class AntiSpam: LocalizedGuildCommand {
    override val name = "antispam"

    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        if (args.isEmpty()) {
            message.reply(texts.error())
            return
        }

        when(args[0].lowercase()) {
            "get", "show" -> message.replyEmbed {
                title = texts["title"]
                description = texts["description"].format(
                    texts["enabled.${config.isEnabled(Features.ANTI_SPAM)}"],
                    texts["enabled.${config.isEnabled(Features.ANTI_SPAM_SILENT)}"],
                    config.antiSpamWarn,
                    config.antiSpamBan
                )

                field(texts["warn.title"], false) { config.antiSpamWarnText.replace("%1\$s", "%user%") }
                field(texts["ban.title"], false) { config.antiSpamBanText }
            }
            "enable" -> {
                transaction { config.enable(Features.ANTI_SPAM) }
                message.reply(texts["enabled"])
            }
            "disable" -> {
                transaction { config.disable(Features.ANTI_SPAM) }
                message.reply(texts.get("disabled"))
            }
            "silent" -> {
                when(args.getOrNull(1)) {
                    "enable" -> {
                        transaction { config.enable(Features.ANTI_SPAM_SILENT) }
                        message.reply(texts["enabled.silent"])
                    }
                    "disable" -> {
                        transaction { config.disable(Features.ANTI_SPAM_SILENT) }
                        message.reply(texts["disabled.silent"])
                    }
                    else -> message.reply(texts.error())
                }
            }
            "warning", "warn" -> {
                val warn = args.drop(1).joinToString(separator = " ").replace("%user%", "%1\$s")
                if (warn.isEmpty()) {
                    message.reply(texts["warn.empty"])
                    return
                }
                transaction { config.antiSpamWarnText = warn }
                message.reply(texts["warn.set"])
            }
            "reason", "ban" -> {
                val ban = args.drop(1).joinToString(separator = " ")
                if (ban.isEmpty()) {
                    message.reply(texts["ban.empty"])
                    return
                }
                transaction { config.antiSpamBanText = ban }
                message.reply(texts["ban.set"])
            }
            "limit" -> {
                when(args.getOrNull(1)) {
                    "ban" -> {
                        val limit = args.getOrNull(2)?.toInt()?.takeIf { it > 1 }
                        if (limit == null) {
                            message.reply(texts["limit.invalid"])
                            return
                        }
                        transaction { config.antiSpamBan = limit }
                        message.reply(texts["limit.ban"].format(limit))
                    }
                    "warn", "warning" -> {
                        val limit = args.getOrNull(2)?.toInt()?.takeIf { it > 1 }
                        if (limit == null) {
                            message.reply(texts["limit.invalid"])
                            return
                        }
                        transaction { config.antiSpamWarn = limit }
                        message.reply(texts["limit.warn"].format(limit))
                    }
                    else -> message.reply(texts.error())
                }
            }
            else -> message.reply(texts.error())
        }
    }
}