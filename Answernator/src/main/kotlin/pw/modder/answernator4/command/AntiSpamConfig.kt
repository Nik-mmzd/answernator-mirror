package pw.modder.answernator4.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.component.interactionButtonAccessory
import dev.kord.rest.builder.component.section
import dev.kord.rest.builder.component.textDisplay
import dev.kord.rest.builder.message.MessageBuilder
import org.kodein.di.DI
import org.kodein.di.instance
import pw.modder.answernator4.db.cache.AntiSpamConfigData
import pw.modder.answernator4.db.cache.AntiSpamConfigRepository
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.button.ButtonField
import pw.modder.answernator4.interaction.button.ButtonGroup
import pw.modder.answernator4.interaction.button.button
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.interaction.modal.Modal
import pw.modder.answernator4.interaction.modal.channelSelect
import pw.modder.answernator4.interaction.modal.getValue
import pw.modder.answernator4.interaction.modal.optional
import pw.modder.answernator4.interaction.modal.provideDelegate
import pw.modder.answernator4.interaction.modal.showModal
import pw.modder.answernator4.interaction.modal.textField
import java.util.ResourceBundle

/**
 * `/anti_spam_config` — an admin panel for a guild's anti-spam configuration.
 *
 * The panel is a Components-V2 ephemeral message: one [section][dev.kord.rest.builder.component.section]
 * per setting, each showing its current value with a button accessory. The two booleans get toggle
 * buttons that flip the value in place; every other setting gets an Edit button that opens a single-field
 * modal. After any toggle or modal submit, the same message is re-rendered from the freshly persisted
 * config. A header warns when the bot lacks the permission a given enforcement step needs.
 *
 * Every button is declared in [Buttons] so the global router in `interactionCommandService` can resolve a
 * click back to [onButtonClick], but they are rendered by hand as section accessories — the button
 * framework only lays buttons out in action rows, not as section accessories.
 */
class AntiSpamConfig(di: DI) : ChatInputCommand(di) {
    override val name = "anti_spam_config"
    override val bundleName = "v4.anti_spam"
    override val defaultMemberPermissions = Permissions(Permission.ManageGuild)
    override val dmPermission = false

    private val repo by di.instance<AntiSpamConfigRepository>()

    override val buttons = Buttons()

    /** The bot's relevant moderation permissions in the invoking guild, read from the interaction. */
    private data class BotPerms(val canTimeout: Boolean, val canBan: Boolean)

    private fun Interaction.botPerms(): BotPerms {
        val perms = data.appPermissions.value
        return BotPerms(
            canTimeout = perms?.contains(Permission.ModerateMembers) ?: false,
            canBan = perms?.contains(Permission.BanMembers) ?: false,
        )
    }

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val guildId = interaction.data.guildId.value
        if (guildId == null) {
            interaction.respondEphemeral { content = bundle.l("antispam.config.guild_only") }
            return
        }

        val config = loadOrDefault(guildId, gbundle)
        val perms = interaction.botPerms()
        interaction.respondEphemeral { renderPanel(gbundle, config, perms) }
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
        val guildId = interaction.data.guildId.value ?: return
        val bundle = gbundle
        val config = loadOrDefault(guildId, bundle)

        when (button) {
            buttons.toggleEnabled -> toggle(bundle, config.copy(isEnabled = !config.isEnabled))
            buttons.toggleMrBeast -> toggle(bundle, config.copy(isMrBeastEnabled = !config.isMrBeastEnabled))

            buttons.editWarningText -> editText(
                bundle, config, "antispam.config.warning_text", config.warningText, paragraph = true,
            ) { c, v -> c.copy(warningText = v) }

            buttons.editMuteText -> editText(
                bundle, config, "antispam.config.mute_text", config.muteText, paragraph = true,
            ) { c, v -> c.copy(muteText = v) }

            buttons.editWarningThreshold -> editInt(
                bundle, config, "antispam.config.warning_threshold", config.warningThreshold, min = 0,
            ) { c, v -> c.copy(warningThreshold = v) }

            buttons.editMuteThreshold -> editInt(
                bundle, config, "antispam.config.mute_threshold", config.muteThreshold, min = 1,
            ) { c, v -> c.copy(muteThreshold = v) }

            buttons.editMutesBeforeBan -> editInt(
                bundle, config, "antispam.config.mutes_before_ban", config.mutesBeforeBan, min = -1,
            ) { c, v -> c.copy(mutesBeforeBan = v) }

            buttons.editMuteDuration -> editInt(
                bundle, config, "antispam.config.mute_duration", config.muteDuration, min = 1,
            ) { c, v -> c.copy(muteDuration = v) }

            buttons.editMuteValidity -> editInt(
                bundle, config, "antispam.config.mute_validity", config.muteValidity, min = 1,
            ) { c, v -> c.copy(muteValidity = v) }

            buttons.editLogChannel -> editLogChannel(bundle, config)

            else -> Unit // link buttons / unknown ids never reach here, but `when` must be exhaustive
        }
    }

    /** Persists a toggled config and re-renders the panel in place (the click owns the response). */
    private suspend fun ButtonInteractionCreateEvent.toggle(bundle: ResourceBundle, updated: AntiSpamConfigData) {
        repo.put(updated)
        val perms = interaction.botPerms()
        interaction.updateEphemeralMessage { renderPanel(bundle, updated, perms) }
    }

    /** Opens a single text-field modal, then persists the trimmed (non-blank) value and re-renders. */
    private suspend fun ButtonInteractionCreateEvent.editText(
        bundle: ResourceBundle,
        config: AntiSpamConfigData,
        labelKey: String,
        current: String,
        paragraph: Boolean,
        apply: (AntiSpamConfigData, String) -> AntiSpamConfigData,
    ) {
        val label = bundle.l(labelKey)
        val modal = TextEditModal(label, current, paragraph, maxLength = TEXT_MAX_LENGTH)
        val reply = interaction.showModal(modal) ?: return

        val text = reply[modal.value].trim()
        if (text.isEmpty()) {
            reply.interaction.respondEphemeral { content = bundle.l("antispam.config.error.blank") }
            return
        }

        val updated = apply(config, text)
        repo.put(updated)
        val perms = reply.interaction.botPerms()
        reply.interaction.updateEphemeralMessage { renderPanel(bundle, updated, perms) }
    }

    /** Opens a single text-field modal expecting an integer ≥ [min]; rejects bad input with an ephemeral error. */
    private suspend fun ButtonInteractionCreateEvent.editInt(
        bundle: ResourceBundle,
        config: AntiSpamConfigData,
        labelKey: String,
        current: Int,
        min: Int,
        apply: (AntiSpamConfigData, Int) -> AntiSpamConfigData,
    ) {
        val label = bundle.l(labelKey)
        val modal = TextEditModal(label, current.toString(), paragraph = false, maxLength = INT_MAX_LENGTH)
        val reply = interaction.showModal(modal) ?: return

        val parsed = reply[modal.value].trim().toIntOrNull()
        if (parsed == null || parsed < min) {
            reply.interaction.respondEphemeral { content = bundle.l("antispam.config.error.invalid_number") }
            return
        }

        val updated = apply(config, parsed)
        repo.put(updated)
        val perms = reply.interaction.botPerms()
        reply.interaction.updateEphemeralMessage { renderPanel(bundle, updated, perms) }
    }

    /** Opens a channel-select modal; an empty selection clears the log channel. */
    private suspend fun ButtonInteractionCreateEvent.editLogChannel(bundle: ResourceBundle, config: AntiSpamConfigData) {
        val modal = ChannelEditModal(bundle.l("antispam.config.log_channel"), config.logChannel)
        val reply = interaction.showModal(modal) ?: return

        val updated = config.copy(logChannel = reply[modal.channel])
        repo.put(updated)
        val perms = reply.interaction.botPerms()
        reply.interaction.updateEphemeralMessage { renderPanel(bundle, updated, perms) }
    }

    private suspend fun loadOrDefault(guildId: Snowflake, bundle: ResourceBundle): AntiSpamConfigData =
        repo.get(guildId) ?: defaultConfig(guildId, bundle)

    // --- rendering -------------------------------------------------------------------------------

    private fun MessageBuilder.renderPanel(bundle: ResourceBundle, config: AntiSpamConfigData, perms: BotPerms) {
        flags = MessageFlags(MessageFlag.IsComponentsV2)
        textDisplay { content = "## ${bundle.l("antispam.config.title")}" }

        permissionWarnings(bundle, config, perms).takeIf { it.isNotEmpty() }?.let { warnings ->
            textDisplay { content = warnings.joinToString("\n") { "⚠️ $it" } }
        }

        toggleSection(bundle, bundle.l("antispam.config.status"), config.isEnabled, buttons.toggleEnabled.id)
        toggleSection(bundle, bundle.l("antispam.config.mrbeast"), config.isMrBeastEnabled, buttons.toggleMrBeast.id)

        editSection(bundle, bundle.l("antispam.config.warning_text"), displayText(config.warningText, bundle), buttons.editWarningText.id)
        editSection(bundle, bundle.l("antispam.config.mute_text"), displayText(config.muteText, bundle), buttons.editMuteText.id)
        editSection(bundle, bundle.l("antispam.config.warning_threshold"), config.warningThreshold.toString(), buttons.editWarningThreshold.id)
        editSection(bundle, bundle.l("antispam.config.mute_threshold"), config.muteThreshold.toString(), buttons.editMuteThreshold.id)
        editSection(bundle, bundle.l("antispam.config.mutes_before_ban"), mutesBeforeBanText(config.mutesBeforeBan, bundle), buttons.editMutesBeforeBan.id)
        editSection(bundle, bundle.l("antispam.config.mute_duration"), bundle.l("antispam.config.minutes").format(config.muteDuration), buttons.editMuteDuration.id)
        editSection(bundle, bundle.l("antispam.config.mute_validity"), bundle.l("antispam.config.days").format(config.muteValidity), buttons.editMuteValidity.id)
        editSection(bundle, bundle.l("antispam.config.log_channel"), config.logChannel?.let { "<#${it.value}>" } ?: bundle.l("antispam.config.none"), buttons.editLogChannel.id)
    }

    /** Lines flagging enforcement that the bot can't actually carry out with its current permissions. */
    private fun permissionWarnings(bundle: ResourceBundle, config: AntiSpamConfigData, perms: BotPerms): List<String> {
        if (!config.isEnabled) return emptyList()
        val warnings = mutableListOf<String>()
        // muteThreshold > 0 means a mute can fire; mutesBeforeBan >= 0 means a ban can fire.
        if (config.muteThreshold > 0 && !perms.canTimeout) warnings += bundle.l("antispam.config.warn.no_timeout")
        if (config.mutesBeforeBan >= 0 && !perms.canBan) warnings += bundle.l("antispam.config.warn.no_ban")
        return warnings
    }

    private fun MessageBuilder.editSection(bundle: ResourceBundle, title: String, value: String, fieldId: String) {
        section {
            textDisplay { content = "**$title**\n$value" }
            interactionButtonAccessory(ButtonStyle.Secondary, "cmd:$effectiveName:$fieldId") {
                label = bundle.l("antispam.config.edit")
            }
        }
    }

    private fun MessageBuilder.toggleSection(bundle: ResourceBundle, title: String, enabled: Boolean, fieldId: String) {
        val value = bundle.l(if (enabled) "antispam.config.on" else "antispam.config.off")
        section {
            textDisplay { content = "**$title**\n$value" }
            interactionButtonAccessory(
                if (enabled) ButtonStyle.Success else ButtonStyle.Secondary,
                "cmd:$effectiveName:$fieldId",
            ) {
                label = bundle.l(if (enabled) "antispam.config.toggle.disable" else "antispam.config.toggle.enable")
            }
        }
    }

    private fun displayText(text: String, bundle: ResourceBundle): String = when {
        text.isBlank() -> bundle.l("antispam.config.none")
        text.length > VALUE_DISPLAY_LIMIT -> text.take(VALUE_DISPLAY_LIMIT - 1) + "…"
        else -> text
    }

    private fun mutesBeforeBanText(value: Int, bundle: ResourceBundle): String = when {
        value < 0 -> bundle.l("antispam.config.mutes_before_ban.never")
        value == 0 -> bundle.l("antispam.config.mutes_before_ban.immediate")
        else -> value.toString()
    }

    companion object {
        /** `warning_text` / `mute_text` columns are `VARCHAR(255)`. */
        private const val TEXT_MAX_LENGTH = 255

        /** Enough digits for any sane threshold/duration. */
        private const val INT_MAX_LENGTH = 7

        /** Truncate long text values in the panel so a section stays readable. */
        private const val VALUE_DISPLAY_LIMIT = 100

        private fun defaultConfig(guildId: Snowflake, bundle: ResourceBundle) = AntiSpamConfigData(
            guildId = guildId,
            isEnabled = false,
            isMrBeastEnabled = false,
            warningText = bundle.l("antispam.warning"),
            muteText = bundle.l("antispam.mute"),
            warningThreshold = 3,
            muteThreshold = 5,
            mutesBeforeBan = 2,
            muteDuration = 60, // minutes
            muteValidity = 30, // days
            logChannel = null,
        )
    }

    class Buttons : ButtonGroup() {
        val toggleEnabled by button(label = "toggle_enabled")
        val toggleMrBeast by button(label = "toggle_mrbeast")
        val editWarningText by button(label = "edit")
        val editMuteText by button(label = "edit")
        val editWarningThreshold by button(label = "edit")
        val editMuteThreshold by button(label = "edit")
        val editMutesBeforeBan by button(label = "edit")
        val editMuteDuration by button(label = "edit")
        val editMuteValidity by button(label = "edit")
        val editLogChannel by button(label = "edit")
    }

    private class TextEditModal(
        label: String,
        current: String,
        paragraph: Boolean,
        maxLength: Int,
    ) : Modal(title = label) {
        val value by textField(
            label = label,
            style = if (paragraph) TextInputStyle.Paragraph else TextInputStyle.Short,
            allowedLength = 1..maxLength,
            defaultValue = current,
        )
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
}