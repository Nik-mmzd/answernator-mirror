package pw.modder.answernator4.interaction.modal

import dev.kord.core.entity.interaction.ModalSubmitInteraction
import kotlin.reflect.KProperty

abstract class Modal(val title: String) {
    private val _elements = mutableListOf<ModalElement>()
    val elements: List<ModalElement> get() = _elements
    val fields: List<ModalField<*>> get() = _elements.filterIsInstance<ModalField<*>>()

    protected fun textDisplay(content: String): TextDisplay {
        val display = TextDisplay(content)
        _elements.add(display)
        return display
    }

    internal fun registerField(field: ModalField<*>) {
        _elements.add(field)
    }
}

@Suppress("UNCHECKED_CAST")
operator fun <T> ModalField<T>.provideDelegate(thisRef: Modal, property: KProperty<*>): ModalField<T> {
    val field = if (id.isEmpty()) (this as ModalFieldImpl<T>).withId(property.name) else this
    thisRef.registerField(field)
    return field
}

operator fun <T> ModalField<T>.getValue(thisRef: Modal, property: KProperty<*>): ModalField<T> = this

class ModalReply(val interaction: ModalSubmitInteraction) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(field: ModalField<T>): T =
        (field as ModalFieldImpl<T>).extractValue(interaction)
}