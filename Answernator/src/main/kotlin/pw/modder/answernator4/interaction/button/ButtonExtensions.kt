package pw.modder.answernator4.interaction.button

import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.interactionButtonAccessory
import dev.kord.rest.builder.component.linkButtonAccessory
import dev.kord.rest.builder.component.section
import dev.kord.rest.builder.message.MessageBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ButtonClickContext(
    val event: ButtonInteractionCreateEvent,
    val button: ButtonField,
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
        ButtonClickContext(this, field, stop).onClick()
    }

    try {
        withTimeoutOrNull(waitFor) { stop.await() }
    } finally {
        job.cancel()
    }
}

private fun MessageBuilder.renderButtons(group: ButtonGroup, baseId: String) {
    flags = MessageFlags(MessageFlag.IsComponentsV2)
    group.content?.let { content = it }
    group.buttons.forEach { field ->
        section {
            textDisplay(field.text)
            when (field) {
                is InteractionButtonField -> interactionButtonAccessory(field.style, "$baseId:${field.id}") {
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
    }
}