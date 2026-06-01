package pw.modder.answernator4.command

import dev.kord.common.Locale
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.component.interactionButtonAccessory
import dev.kord.rest.builder.component.section
import dev.kord.rest.builder.component.textDisplay
import dev.kord.rest.builder.message.MessageBuilder
import org.kodein.di.DI
import org.kodein.di.instance
import pw.modder.answernator4.db.cache.LogsConfigData
import pw.modder.answernator4.db.cache.LogsConfigRepository
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.SUPPORTED_LOCALES
import pw.modder.answernator4.interaction.button.ButtonField
import pw.modder.answernator4.interaction.button.ButtonGroup
import pw.modder.answernator4.interaction.button.button
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.interaction.modal.Modal
import pw.modder.answernator4.interaction.modal.SelectChoice
import pw.modder.answernator4.interaction.modal.channelSelect
import pw.modder.answernator4.interaction.modal.getValue
import pw.modder.answernator4.interaction.modal.optional
import pw.modder.answernator4.interaction.modal.provideDelegate
import pw.modder.answernator4.interaction.modal.radioGroup
import pw.modder.answernator4.interaction.modal.showModal
import java.util.ResourceBundle

/**
 * `/logs_config` — an admin panel for a guild's logging configuration, built like [AntiSpamConfig].
 *
 * The panel is a Components-V2 ephemeral message: one [section][dev.kord.rest.builder.component.section]
 * per setting with its current value and an Edit button accessory. Each log channel's Edit button opens a
 * channel-select modal (an empty selection clears the channel, disabling that log); the locale's Edit
 * button opens a radio modal over the supported locales. After any modal submit the same message is
 * re-rendered from the freshly persisted config.
 *
 * Every button is declared in [Buttons] so the global router in `interactionCommandService` resolves a
 * click back to [onButtonClick], but they are rendered by hand as section accessories.
 */
class GuildLogsConfig(di: DI) : ChatInputCommand(di) {
    override val name = "logs_config"
    override val bundleName = "v4.guild_log"
    override val defaultMemberPermissions = Permissions(Permission.ManageGuild)
    override val dmPermission = false

    private val repo by di.instance<LogsConfigRepository>()

    override val buttons = Buttons()

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val guildId = interaction.data.guildId.value
        if (guildId == null) {
            interaction.respondEphemeral { content = bundle.l("logs.config.guild_only") }
            return
        }

        val config = loadOrDefault(guildId, interaction.guildLocale)
        interaction.respondEphemeral { renderPanel(gbundle, config) }
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
        val guildId = interaction.data.guildId.value ?: return
        val bundle = gbundle
        val config = loadOrDefault(guildId, interaction.guildLocale)

        when (button) {
            buttons.editMemberJoin -> editChannel(bundle, config, "logs.config.member_join", config.memberJoinChannel) { c, v -> c.copy(memberJoinChannel = v) }
            buttons.editMemberLeave -> editChannel(bundle, config, "logs.config.member_leave", config.memberLeaveChannel) { c, v -> c.copy(memberLeaveChannel = v) }
            buttons.editMemberBan -> editChannel(bundle, config, "logs.config.member_ban", config.memberBanLogChannel) { c, v -> c.copy(memberBanLogChannel = v) }
            buttons.editMemberUnban -> editChannel(bundle, config, "logs.config.member_unban", config.memberUnbanLogChannel) { c, v -> c.copy(memberUnbanLogChannel = v) }
            buttons.editMemberMute -> editChannel(bundle, config, "logs.config.member_mute", config.memberMuteLogChannel) { c, v -> c.copy(memberMuteLogChannel = v) }
            buttons.editMemberUpdate -> editChannel(bundle, config, "logs.config.member_update", config.memberUpdateLogChannel) { c, v -> c.copy(memberUpdateLogChannel = v) }
            buttons.editLocale -> editLocale(bundle, config)

            else -> Unit // link buttons / unknown ids never reach here, but `when` must be exhaustive
        }
    }

    /** Opens a channel-select modal scoped to [labelKey]; an empty selection clears that log channel. */
    private suspend fun ButtonInteractionCreateEvent.editChannel(
        bundle: ResourceBundle,
        config: LogsConfigData,
        labelKey: String,
        current: Snowflake?,
        apply: (LogsConfigData, Snowflake?) -> LogsConfigData,
    ) {
        val modal = ChannelEditModal(bundle.l(labelKey), current)
        val reply = interaction.showModal(modal) ?: return

        val updated = apply(config, reply[modal.channel])
        repo.put(updated)
        reply.interaction.updateEphemeralMessage { renderPanel(bundle, updated) }
    }

    /** Opens a radio modal over the supported locales and persists the chosen one. */
    private suspend fun ButtonInteractionCreateEvent.editLocale(bundle: ResourceBundle, config: LogsConfigData) {
        val choices = SUPPORTED_LOCALES.map {
            SelectChoice(label = localeDisplay(it), value = it.toCode(), default = it.toCode() == config.locale.toCode())
        }
        val modal = LocaleEditModal(bundle.l("logs.config.locale"), choices)
        val reply = interaction.showModal(modal) ?: return

        val updated = config.copy(locale = Locale.fromString(reply[modal.locale]))
        repo.put(updated)
        reply.interaction.updateEphemeralMessage { renderPanel(bundle, updated) }
    }

    private suspend fun loadOrDefault(guildId: Snowflake, guildLocale: Locale?): LogsConfigData =
        repo.get(guildId) ?: defaultConfig(guildId, guildLocale)

    // --- rendering -------------------------------------------------------------------------------

    private fun MessageBuilder.renderPanel(bundle: ResourceBundle, config: LogsConfigData) {
        flags = MessageFlags(MessageFlag.IsComponentsV2)
        textDisplay { content = "## ${bundle.l("logs.config.title")}" }

        channelSection(bundle, bundle.l("logs.config.member_join"), config.memberJoinChannel, buttons.editMemberJoin.id)
        channelSection(bundle, bundle.l("logs.config.member_leave"), config.memberLeaveChannel, buttons.editMemberLeave.id)
        channelSection(bundle, bundle.l("logs.config.member_ban"), config.memberBanLogChannel, buttons.editMemberBan.id)
        channelSection(bundle, bundle.l("logs.config.member_unban"), config.memberUnbanLogChannel, buttons.editMemberUnban.id)
        channelSection(bundle, bundle.l("logs.config.member_mute"), config.memberMuteLogChannel, buttons.editMemberMute.id)
        channelSection(bundle, bundle.l("logs.config.member_update"), config.memberUpdateLogChannel, buttons.editMemberUpdate.id)

        section {
            textDisplay { content = "**${bundle.l("logs.config.locale")}**\n${localeDisplay(config.locale)}" }
            interactionButtonAccessory(ButtonStyle.Secondary, "cmd:$effectiveName:${buttons.editLocale.id}") {
                label = bundle.l("logs.config.edit")
            }
        }
    }

    private fun MessageBuilder.channelSection(bundle: ResourceBundle, title: String, channel: Snowflake?, fieldId: String) {
        val value = channel?.let { "<#${it.value}>" } ?: bundle.l("logs.config.disabled")
        section {
            textDisplay { content = "**$title**\n$value" }
            interactionButtonAccessory(ButtonStyle.Secondary, "cmd:$effectiveName:$fieldId") {
                label = bundle.l("logs.config.edit")
            }
        }
    }

    /** A locale's own-language name (autonym), shown regardless of the viewer's locale. */
    private fun localeDisplay(locale: Locale): String = when (locale.language) {
        "ru" -> "Русский"
        else -> "English"
    }

    companion object {
        /** Renders a [Locale] as the `language[-COUNTRY]` code the [LogsConfigs][pw.modder.answernator4.db.tables.LogsConfigs] column stores. */
        private fun Locale.toCode(): String = "$language${country?.let { "-$it" } ?: ""}"

        private fun defaultConfig(guildId: Snowflake, guildLocale: Locale?): LogsConfigData {
            val locale = guildLocale?.takeIf { it in SUPPORTED_LOCALES } ?: SUPPORTED_LOCALES.first()
            return LogsConfigData(
                guildId = guildId,
                memberJoinChannel = null,
                memberLeaveChannel = null,
                memberBanLogChannel = null,
                memberUnbanLogChannel = null,
                memberMuteLogChannel = null,
                memberUpdateLogChannel = null,
                locale = locale,
            )
        }
    }

    class Buttons : ButtonGroup() {
        val editMemberJoin by button(label = "edit")
        val editMemberLeave by button(label = "edit")
        val editMemberBan by button(label = "edit")
        val editMemberUnban by button(label = "edit")
        val editMemberMute by button(label = "edit")
        val editMemberUpdate by button(label = "edit")
        val editLocale by button(label = "edit")
    }

    private class ChannelEditModal(
        label: String,
        current: Snowflake?,
    ) : Modal(title = label) {
        val channel by channelSelect(
            label = label,
            channelTypes = listOf(ChannelType.GuildText, ChannelType.GuildNews),
            defaultChannels = current?.let { listOf(it) } ?: emptyList(),
        ).optional()
    }

    private class LocaleEditModal(
        label: String,
        choices: List<SelectChoice>,
    ) : Modal(title = label) {
        val locale by radioGroup(label = label, choices = choices)
    }
}