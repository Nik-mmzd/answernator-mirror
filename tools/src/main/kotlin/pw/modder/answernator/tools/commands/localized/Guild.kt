package pw.modder.answernator.tools.commands.localized

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.kord.instant
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Guild: LocalizedGuildCommand {
    override val name = "guild"
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(message: Message, args: List<String>, msgGuild: Guild, texts: CommandLocaleBundle) {
        if (args.first().equals("list", true)) {
            message.reply(message.kord.guilds.toList().joinToString("\n", prefix = texts.getString("list.available")) {
                "${it.name}: `${it.id.asString}`"
            })
            return
        }

        val guild = when {
            args.isNotEmpty() -> message.kord.guilds.firstOrNull { it.id.asString == args.first() }
                ?: message.kord.guilds.firstOrNull { it.name.contains(args.joinToString(separator = " "), true) }
            else -> msgGuild
        }

        if (guild == null) {
            message.reply(texts.getErrorString())
            return
        }

        message.reply {
            embed {
                title = texts.formatString("title", guild.name)

                field(texts.getString("owner"), true) { guild.owner.mention }
                field(texts.getString("emojis"), true) { guild.emojis.count().toString() }
                if (guild.roles.count() < 50 && guild.id == msgGuild.id) {
                    field(texts.getString("roles"), false) {
                        guild.roles.filterNot { it.id == guild.id }.toList().joinToString(" ") { it.mention }
                    }
                } else {
                    field(texts.getString("roles"), true) { (guild.roles.count() - 1).toString() }
                }
                field(texts.getString("created_at"), false) {
                    texts.formatString("created_at.value", Utils.prettyPrintPeriod(texts.locale, guild.id.instant))
                }
                field(texts.getString("region"), true) {
                    guild.getRegion().name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                field(texts.getString("features"), true) {
                    guild.features.joinToString(", ") {
                        texts.getString("features.${it.value}")
                    }.ifEmpty { texts.getString("features.empty") }
                }
                field(texts.getString("verificationLevel"), true) {
                    texts.getString("verification.level.${guild.verificationLevel.value}")
                }
                field(texts.getString("mfaEnabled"), true) {
                    texts.getString("mfa.${guild.mfaLevel.value}")
                }
                field(texts.getString("explicitContentFilterLevel"), true) {
                    texts.getString("explicitContentFilterLevel.${guild.contentFilter.value}")
                }
                guild.iconHash?.run {
                    thumbnail { url = "https://cdn.discordapp.com/icons/${guild.id}/$this" }
                }
                guild.afkChannel?.run {
                    field(texts.getString("afkChannel"), true) { mention }
                    field(texts.getString("afkTimeout"), true) {
                        texts.formatString("afkTimeout.value", guild.afkTimeout)
                    }
                }

                field(texts.getString("notifications"), true) {
                    texts.getString("notifications.level.${guild.defaultMessageNotificationLevel.value}")
                }
                if (guild.isWidgetEnabled) {
                    field(texts.getString("widget"), true) {
                        texts.getString("widget.${guild.isWidgetEnabled}")
                    }
                    guild.widgetChannel?.run {
                        field(texts.getString("widget.channel"), true) { mention }
                    }
                }

                guild.systemChannel?.run {
                    field(texts.getString("system.channel"), true) { mention }
                }
            }
            allowedMentions { repliedUser = false }
        }
    }
}