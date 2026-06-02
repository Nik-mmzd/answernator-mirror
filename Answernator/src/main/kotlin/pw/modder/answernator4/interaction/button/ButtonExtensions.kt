package pw.modder.answernator4.interaction.button

import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.textDisplay
import dev.kord.rest.builder.message.MessageBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import pw.modder.answernator4.interaction.Command
import pw.modder.answernator4.interaction.l
import java.util.ResourceBundle
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ButtonClickContext(
    val event: ButtonInteractionCreateEvent,
    val button: ButtonField,
    /**
     * The `${baseId}` half of the clicked component's customId — the same value used at render time
     * by [respondWithButtons]. Useful for [renderButtons] when re-rendering the message on click
     * (e.g. a "reroll" button that needs to keep the same listener wiring).
     */
    val baseId: String,
    private val stopSignal: CompletableDeferred<Unit>,
) {
    val interaction: ButtonInteraction get() = event.interaction

    fun stop() {
        stopSignal.complete(Unit)
    }
}

suspend fun ActionInteractionBehavior.respondWithButtons(
    group: ButtonGroup,
    ephemeral: Boolean = true,
    waitFor: Duration = 5.minutes,
    bundle: ResourceBundle? = null,
    onClick: suspend ButtonClickContext.() -> Unit = {},
) {
    val baseId = "${group::class.simpleName}:${UUID.randomUUID()}"
    val buttonsByCustomId = group.buttons
        .filterIsInstance<InteractionButtonField>()
        .associateBy { "$baseId:${it.id}" }

    if (ephemeral) {
        respondEphemeral { renderButtons(group, baseId, bundle = bundle) }
    } else {
        respondPublic { renderButtons(group, baseId, bundle = bundle) }
    }

    val stop = CompletableDeferred<Unit>()
    val job = kord.on<ButtonInteractionCreateEvent> {
        val field = buttonsByCustomId[interaction.componentId] ?: return@on
        ButtonClickContext(this, field, baseId, stop).onClick()
    }

    try {
        withTimeoutOrNull(waitFor) { stop.await() }
    } finally {
        job.cancel()
    }
}

/**
 * Renders the [command]'s [Command.buttons] as the interaction response without waiting for clicks.
 * Click events are routed to [Command.onButtonClick] by the global listener in `interactionCommandService`.
 *
 * Use this when the button view's lifetime should be independent of the command coroutine —
 * e.g. a refreshable bot-status panel.
 *
 * @param state optional payload appended to each button's customId (`cmd:{name}:{buttonId}:{state}`).
 * The same value is delivered back to [Command.onButtonClick] with state on click. Use it to carry
 * per-invocation context (e.g. an expression to re-evaluate) without persisting anywhere. Discord
 * caps customId at 100 chars total; the caller is responsible for keeping `state` short enough.
 * @param content optional text-display content rendered above the buttons; if `null`, the group's
 * own [ButtonGroup.content] is used.
 * @param bundle optional locale bundle used to resolve button labels that are localization keys;
 * if `null`, labels are rendered literally.
 * @param fields optional explicit set of fields to render instead of the group's full list. Pass
 * per-render copies (e.g. `buttons.detail.visibleIf(expanded)`) to toggle visibility without
 * mutating the shared group instance. Routing still uses the group's full list, so hidden buttons
 * keep their slot. Defaults to [Command.buttons]'s declared buttons.
 *
 * @throws IllegalStateException if [command] doesn't declare [Command.buttons].
 */
suspend fun ActionInteractionBehavior.respondWithCommandButtons(
    command: Command,
    ephemeral: Boolean = true,
    state: String? = null,
    content: String? = null,
    bundle: ResourceBundle? = null,
    fields: List<ButtonField>? = null,
) {
    val group = command.buttons
        ?: error("Command '${command.name}' has no buttons declared")
    val baseId = "cmd:${command.effectiveName}"
    val renderFields = fields ?: group.buttons
    val effectiveContent = content ?: group.content

    if (ephemeral) {
        respondEphemeral { renderButtons(renderFields, baseId, state, effectiveContent, bundle) }
    } else {
        respondPublic { renderButtons(renderFields, baseId, state, effectiveContent, bundle) }
    }
}

/**
 * Adds the [group]'s buttons to this classic (non-V2) [ActionRowBuilder]. Use this when composing
 * a legacy message that mixes buttons with other content Discord doesn't allow under V2 — most
 * commonly an `embed` alongside an `actionRow`:
 *
 * ```kotlin
 * respond {
 *     embed { ... }
 *     actionRow { renderButtons(buttons, baseId = "cmd:$effectiveName") }
 * }
 * ```
 *
 * A single action row holds at most five buttons; the caller is responsible for not overflowing it.
 * For V2 messages (no embed), use the [MessageBuilder] overload, which wraps automatically.
 *
 * @param baseId the customId prefix, e.g. `"cmd:${command.effectiveName}"`.
 * @param state optional payload appended to each interaction button's customId
 * (`${baseId}:${field.id}:${state}`); the router splits on `:` and forwards it to [Command.onButtonClick].
 * @param bundle optional locale bundle; button labels that are localization keys are resolved
 * against it (a missing key falls through to the literal). If `null`, labels are rendered literally.
 */
fun ActionRowBuilder.renderButtons(
    group: ButtonGroup,
    baseId: String,
    state: String? = null,
    bundle: ResourceBundle? = null,
) = renderButtons(group.buttons, baseId, state, bundle)

/**
 * Like the [ButtonGroup] overload, but renders an explicit list of [fields]. Pass per-render copies
 * (e.g. `buttons.detail.visibleIf(expanded)`) to toggle visibility per invocation without mutating
 * a shared group. Hidden fields (`visible == false`) are skipped.
 */
fun ActionRowBuilder.renderButtons(
    fields: List<ButtonField>,
    baseId: String,
    state: String? = null,
    bundle: ResourceBundle? = null,
) = addButtons(fields.filter { it.visible }, baseId, state?.let { ":$it" } ?: "", bundle)

private fun ActionRowBuilder.addButtons(
    fields: List<ButtonField>,
    baseId: String,
    stateSuffix: String,
    bundle: ResourceBundle?,
) {
    fun ButtonField.resolvedLabel(): String? = label?.let { bundle?.l(it) ?: it }
    fields.forEach { field ->
        when (field) {
            is InteractionButtonField -> interactionButton(field.style, "$baseId:${field.id}$stateSuffix") {
                field.resolvedLabel()?.let { label = it }
                field.emoji?.let { emoji = it }
                if (field.disabled) disabled = true
            }
            is LinkButtonField -> linkButton(field.url) {
                field.resolvedLabel()?.let { label = it }
                field.emoji?.let { emoji = it }
                if (field.disabled) disabled = true
            }
        }
    }
}

/**
 * Renders [group] into the current [MessageBuilder] as a V2-component message: an optional
 * top-level text-display for content, followed by the buttons laid out in V2 ActionRows of up to
 * five buttons each (preserving declaration order). The component-V2 message flag is set
 * automatically.
 *
 * Use this when responding outside [respondWithButtons] / [respondWithCommandButtons] — most
 * commonly inside a click handler that wants to re-render the same view (e.g. a "reroll" button
 * that changes the displayed content but keeps the buttons live). When the message also needs an
 * `embed` or other non-V2 content, drop down to the [ActionRowBuilder] overload instead.
 *
 * @param baseId the customId prefix. Pass the same value you received from
 * [ButtonClickContext.baseId] (suspending pattern) or `"cmd:${command.effectiveName}"` (stateless).
 * @param state optional payload appended to each interaction button's customId. With state, the
 * customId becomes `${baseId}:${field.id}:${state}`; without, `${baseId}:${field.id}`. The router
 * splits on `:` and forwards the state to [Command.onButtonClick].
 * @param contentOverride if non-`null`, used as the top-level text-display content instead of
 * [ButtonGroup.content]. Useful for stateless commands where the visible body changes per render
 * while the [ButtonGroup] itself is class-level and static.
 * @param bundle optional locale bundle; button labels that are localization keys are resolved
 * against it (a missing key falls through to the literal). If `null`, labels are rendered literally.
 */
fun MessageBuilder.renderButtons(
    group: ButtonGroup,
    baseId: String,
    state: String? = null,
    contentOverride: String? = null,
    bundle: ResourceBundle? = null,
) = renderButtons(group.buttons, baseId, state, contentOverride ?: group.content, bundle)

/**
 * Like the [ButtonGroup] overload, but renders an explicit list of [fields]. Pass per-render copies
 * (e.g. `buttons.detail.visibleIf(expanded)`) to toggle visibility per invocation without mutating
 * a shared group. Hidden fields (`visible == false`) are skipped; the rest wrap into V2 ActionRows
 * of five. Note there's no [ButtonGroup.content] fallback here — pass [contentOverride] explicitly.
 */
fun MessageBuilder.renderButtons(
    fields: List<ButtonField>,
    baseId: String,
    state: String? = null,
    contentOverride: String? = null,
    bundle: ResourceBundle? = null,
) {
    flags = MessageFlags(MessageFlag.IsComponentsV2)
    contentOverride?.let { text -> textDisplay { content = text } }
    val stateSuffix = state?.let { ":$it" } ?: ""

    fields.filter { it.visible }.chunked(5).forEach { chunk ->
        actionRow { addButtons(chunk, baseId, stateSuffix, bundle) }
    }
}