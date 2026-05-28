package pw.modder.answernator4.interaction.button

import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.interactionButtonAccessory
import dev.kord.rest.builder.component.linkButtonAccessory
import dev.kord.rest.builder.component.section
import dev.kord.rest.builder.component.textDisplay
import dev.kord.rest.builder.message.MessageBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import pw.modder.answernator4.interaction.Command
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
    onClick: suspend ButtonClickContext.() -> Unit = {},
) {
    val baseId = "${group::class.simpleName}:${UUID.randomUUID()}"
    val buttonsByCustomId = group.buttons
        .filterIsInstance<InteractionButtonField>()
        .associateBy { "$baseId:${it.id}" }

    if (ephemeral) {
        respondEphemeral { renderButtons(group, baseId) }
    } else {
        respondPublic { renderButtons(group, baseId) }
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
 *
 * @throws IllegalStateException if [command] doesn't declare [Command.buttons].
 */
suspend fun ActionInteractionBehavior.respondWithCommandButtons(
    command: Command,
    ephemeral: Boolean = true,
    state: String? = null,
    content: String? = null,
) {
    val group = command.buttons
        ?: error("Command '${command.name}' has no buttons declared")
    val baseId = "cmd:${command.effectiveName}"

    if (ephemeral) {
        respondEphemeral { renderButtons(group, baseId, state, content) }
    } else {
        respondPublic { renderButtons(group, baseId, state, content) }
    }
}

/**
 * Renders [group] into the current [MessageBuilder] as a V2-component message: a top-level
 * text-display for content (if any) plus a single [Section][dev.kord.rest.builder.component.SectionBuilder]
 * per button. The component-V2 message flag is set automatically.
 *
 * Use this when responding outside [respondWithButtons] / [respondWithCommandButtons] — most
 * commonly inside a click handler that wants to re-render the same view (e.g. a "reroll" button
 * that changes the displayed content but keeps the buttons live).
 *
 * @param baseId the customId prefix. Pass the same value you received from
 * [ButtonClickContext.baseId] (suspending pattern) or `"cmd:${command.effectiveName}"` (stateless).
 * @param state optional payload appended to each interaction button's customId. With state, the
 * customId becomes `${baseId}:${field.id}:${state}`; without, `${baseId}:${field.id}`. The router
 * splits on `:` and forwards the state to [Command.onButtonClick].
 * @param contentOverride if non-`null`, used as the top-level text-display content instead of
 * [ButtonGroup.content]. Useful for stateless commands where the visible body changes per render
 * while the [ButtonGroup] itself is class-level and static.
 */
fun MessageBuilder.renderButtons(
    group: ButtonGroup,
    baseId: String,
    state: String? = null,
    contentOverride: String? = null,
) {
    flags = MessageFlags(MessageFlag.IsComponentsV2)
    val effectiveContent = contentOverride ?: group.content
    effectiveContent?.let { text -> textDisplay { content = text } }
    val stateSuffix = state?.let { ":$it" } ?: ""

    // Buttons with `text` render as V2 Sections (one per button, text-component + accessory).
    // Buttons without `text` render as V2 ActionRows of up to 5 buttons each, preserving order.
    val pendingRow = mutableListOf<ButtonField>()
    fun flushRow() {
        if (pendingRow.isEmpty()) return
        val rowButtons = pendingRow.toList()
        pendingRow.clear()
        actionRow {
            rowButtons.forEach { field ->
                when (field) {
                    is InteractionButtonField -> interactionButton(field.style, "$baseId:${field.id}$stateSuffix") {
                        field.label?.let { label = it }
                        field.emoji?.let { emoji = it }
                        if (field.disabled) disabled = true
                    }
                    is LinkButtonField -> linkButton(field.url) {
                        field.label?.let { label = it }
                        field.emoji?.let { emoji = it }
                        if (field.disabled) disabled = true
                    }
                }
            }
        }
    }

    group.buttons.forEach { field ->
        if (field.text != null) {
            flushRow()
            section {
                textDisplay(field.text!!)
                when (field) {
                    is InteractionButtonField -> interactionButtonAccessory(field.style, "$baseId:${field.id}$stateSuffix") {
                        field.label?.let { label = it }
                        field.emoji?.let { emoji = it }
                        if (field.disabled) disabled = true
                    }
                    is LinkButtonField -> linkButtonAccessory(field.url) {
                        field.label?.let { label = it }
                        field.emoji?.let { emoji = it }
                        if (field.disabled) disabled = true
                    }
                }
            }
        } else {
            pendingRow.add(field)
            if (pendingRow.size == 5) flushRow()
        }
    }
    flushRow()
}