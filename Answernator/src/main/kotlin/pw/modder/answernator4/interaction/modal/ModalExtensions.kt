package pw.modder.answernator4.interaction.modal

import dev.kord.core.behavior.interaction.ModalParentInteractionBehavior
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

suspend fun ModalParentInteractionBehavior.showModal(
    form: Modal,
    waitFor: Duration = 5.minutes,
): ModalReply? {
    val customId = "${form::class.simpleName}:${UUID.randomUUID()}"

    modal(form.title, customId) {
        form.elements.forEach { element ->
            when (element) {
                is TextDisplay -> textDisplay { content = element.content }
                is ModalField<*> -> label(element.label) {
                    element.description?.let { description = it }
                    element.buildIn(this)
                }
            }
        }
    }

    val deferred = CompletableDeferred<ModalSubmitInteraction>()
    val job = kord.on<ModalSubmitInteractionCreateEvent> {
        if (interaction.modalId == customId) {
            deferred.complete(interaction)
        }
    }

    return try {
        withTimeoutOrNull(waitFor) { deferred.await() }?.let { ModalReply(it) }
    } finally {
        job.cancel()
    }
}
