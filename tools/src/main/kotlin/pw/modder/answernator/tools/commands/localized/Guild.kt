package pw.modder.answernator.tools.commands.localized

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.kord.instant
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Guild: LocalizedGuildCommand {
    override val name = "guild"
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        if (args.firstOrNull().equals("list", true)) {
            message.reply(message.kord.guilds.toList().joinToString("\n", prefix = texts.getString("list.available")) {
                "${it.name}: `${it.id.asString}`"
            })
            return
        }

        val givenGuild = when {
            args.isNotEmpty() -> message.kord.guilds.firstOrNull { it.id.asString == args.first() }
                ?: message.kord.guilds.firstOrNull { it.name.contains(args.joinToString(separator = " "), true) }
            else -> guild
        }

        if (givenGuild == null) {
            message.reply(texts.getErrorString())
            return
        }

        message.replyEmbed {
            title = texts.formatString("title", givenGuild.name)

            field(texts.getString("owner"), true) { givenGuild.owner.mention }
            field(texts.getString("emojis"), true) { givenGuild.emojis.count().toString() }
            if (givenGuild.roles.count() < 50 && givenGuild.id == guild.id) {
                field(texts.getString("roles"), false) {
                    givenGuild.roles.filterNot { it.id == givenGuild.id }.toList().joinToString(" ") { it.mention }
                }
            } else {
                field(texts.getString("roles"), true) { (givenGuild.roles.count() - 1).toString() }
            }
            field(texts.getString("created_at"), false) {
                texts.formatString("created_at.value", Utils.prettyPrintPeriod(texts.locale, givenGuild.id.instant))
            }
            field(texts.getString("region"), true) {
                givenGuild.getRegion().name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            field(texts.getString("features"), true) {
                givenGuild.features.joinToString(", ") {
                    texts.getString("features.${it.value}")
                }.ifEmpty { texts.getString("features.empty") }
            }
            field(texts.getString("verificationLevel"), true) {
                texts.getString("verification.level.${givenGuild.verificationLevel.value}")
            }
            field(texts.getString("mfaEnabled"), true) {
                texts.getString("mfa.${givenGuild.mfaLevel.value}")
            }
            field(texts.getString("explicitContentFilterLevel"), true) {
                texts.getString("explicitContentFilterLevel.${givenGuild.contentFilter.value}")
            }
            givenGuild.iconHash?.run {
                thumbnail { url = "https://cdn.discordapp.com/icons/${givenGuild.id}/$this" }
            }
            givenGuild.afkChannel?.run {
                field(texts.getString("afkChannel"), true) { mention }
                field(texts.getString("afkTimeout"), true) {
                    texts.formatString("afkTimeout.value", givenGuild.afkTimeout)
                }
            }

            field(texts.getString("notifications"), true) {
                texts.getString("notifications.level.${givenGuild.defaultMessageNotificationLevel.value}")
            }
            if (givenGuild.isWidgetEnabled) {
                field(texts.getString("widget"), true) {
                    texts.getString("widget.${givenGuild.isWidgetEnabled}")
                }
                givenGuild.widgetChannel?.run {
                    field(texts.getString("widget.channel"), true) { mention }
                }
            }

            givenGuild.systemChannel?.run {
                field(texts.getString("system.channel"), true) { mention }
            }
        }
    }
}