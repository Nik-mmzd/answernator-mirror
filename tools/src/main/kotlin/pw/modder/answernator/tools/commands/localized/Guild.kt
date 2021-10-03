package pw.modder.answernator.tools.commands.localized

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.TimestampFormat
import pw.modder.answernator.utils.extensions.kord.*
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Guild: LocalizedGuildCommand {
    override val name = "guild"
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        if (args.firstOrNull().equals("list", true)) {
            message.reply(message.kord.guilds.toList().joinToString("\n", prefix = texts["list.available"]) {
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
            message.reply(texts.error())
            return
        }

        message.replyEmbed {
            title = texts["title"].format(givenGuild.name)

            field(texts["owner"], true) { givenGuild.owner.mention }
            field(texts["emojis"], true) { givenGuild.emojis.count().toString() }
            if (givenGuild.roles.count() < 50 && givenGuild.id == guild.id) {
                field(texts["roles"], false) {
                    givenGuild.roles.toList()
                        .filterNot { it.id == givenGuild.id }.sortedByDescending { it.rawPosition }
                        .joinToString(" ") { it.mention }
                        .ifEmpty { texts["roles.none"] }
                }
            } else {
                field(texts["roles"], true) { (givenGuild.roles.count() - 1).toString() }
            }
            field(texts["created_at"], false) {
                "${givenGuild.id.timestampMention} (${givenGuild.id.timestampMention(TimestampFormat.RELATIVE)})"
            }
            field(texts["region"], true) {
                givenGuild.getRegion().name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            field(texts["features"], true) {
                givenGuild.features.joinToString(", ") {
                    texts.getOrNull("features.${it.value}") ?: it.value
                }.ifEmpty { texts["features.empty"] }
            }
            field(texts["verificationLevel"], true) {
                texts["verification.level.${givenGuild.verificationLevel.value}"]
            }
            field(texts["mfaEnabled"], true) {
                texts["mfa.${givenGuild.mfaLevel.value}"]
            }
            field(texts["explicitContentFilterLevel"], true) {
                texts["explicitContentFilterLevel.${givenGuild.contentFilter.value}"]
            }
            givenGuild.iconHash?.run {
                thumbnail { url = "https://cdn.discordapp.com/icons/${givenGuild.id}/$this" }
            }
            givenGuild.afkChannel?.run {
                field(texts["afkChannel"], true) { mention }
                field(texts["afkTimeout"], true) {
                    texts["afkTimeout.value"].format(givenGuild.afkTimeout)
                }
            }

            field(texts["notifications"], true) {
                texts["notifications.level.${givenGuild.defaultMessageNotificationLevel.value}"]
            }
            if (givenGuild.isWidgetEnabled) {
                field(texts["widget"], true) {
                    texts["widget.${givenGuild.isWidgetEnabled}"]
                }
                givenGuild.widgetChannel?.run {
                    field(texts["widget.channel"], true) { mention }
                }
            }

            givenGuild.systemChannel?.run {
                field(texts["system.channel"], true) { mention }
            }
        }
    }
}