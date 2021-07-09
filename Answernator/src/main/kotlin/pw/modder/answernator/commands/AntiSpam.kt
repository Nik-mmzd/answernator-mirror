package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.words
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import com.jessecorbett.diskord.dsl.message as dslmessage

class AntiSpam: LocalizedGuildCommand {
    override val name = "antispam"

    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(
        bot: Bot,
        message: Message,
        texts: CommandLocaleBundle,
        guildId: String
    ): CombinedMessageEmbed {
        val config = Db.getAntiSpamConfig(guildId)

        if (message.words.size < 2)
            return texts.getErrorString().toMessage()

        return when(message.words[1].toLowerCase()) {
            "get", "show" -> dslmessage {
                title = texts.getString("title")
                description = texts.formatString("description",
                    texts.getString("enabled.${config.isEnabled(Features.ANTI_SPAM)}"),
                    texts.getString("enabled.${config.isEnabled(Features.ANTI_SPAM_SILENT)}"),
                    config.antiSpamWarn,
                    config.antiSpamBan
                )

                field(texts.getString("warn.title"), config.antiSpamWarnText.replace("%1\$s", "%user%"), false)
                field(texts.getString("ban.title"), config.antiSpamBanText, false)
            }
            "enable" -> {
                transaction { config.enable(Features.ANTI_SPAM) }
                texts.getString("enabled").toMessage()
            }
            "disable" -> {
                transaction { config.disable(Features.ANTI_SPAM) }
                texts.getString("disabled").toMessage()
            }
            "silent" -> {
                when(message.words.getOrNull(2)) {
                    "enable" -> {
                        transaction { config.enable(Features.ANTI_SPAM_SILENT) }
                        texts.getString("enabled.silent").toMessage()
                    }
                    "disable" -> {
                        transaction { config.disable(Features.ANTI_SPAM_SILENT) }
                        texts.getString("disabled.silent").toMessage()
                    }
                    else -> texts.getErrorString().toMessage()
                }
            }
            "warning", "warn" -> {
                val warn = message.words.drop(2).joinToString(separator = " ").replace("%user%", "%1\$s")
                if (warn.isEmpty()) return texts.getString("warn.empty").toMessage()
                transaction { config.antiSpamWarnText = warn }
                texts.getString("warn.set").toMessage()
            }
            "reason", "ban" -> {
                val ban = message.words.drop(2).joinToString(separator = " ")
                if (ban.isEmpty()) return texts.getString("ban.empty").toMessage()
                transaction { config.antiSpamBanText = ban }
                texts.getString("ban.set").toMessage()
            }
            "limit" -> {
                when(message.words.getOrNull(2)) {
                    "ban" -> {
                        val limit = message.words.getOrNull(3)?.toInt()?.takeIf { it > 1 }
                            ?: return texts.getString("limit.invalid").toMessage()
                        transaction { config.antiSpamBan = limit }
                        texts.formatString("limit.ban", limit).toMessage()
                    }
                    "warn", "warning" -> {
                        val limit = message.words.getOrNull(3)?.toInt()?.takeIf { it > 1 }
                            ?: return texts.getString("limit.invalid").toMessage()
                        transaction { config.antiSpamWarn = limit }
                        texts.formatString("limit.warn", limit).toMessage()
                    }
                    else -> texts.getErrorString().toMessage()
                }
            }
            else -> texts.getErrorString().toMessage()
        }
    }
}