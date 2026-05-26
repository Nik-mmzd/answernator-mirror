package pw.modder.answernator4.interaction.button

import kotlin.reflect.KProperty

abstract class ButtonGroup(val content: String? = null) {
    private val _buttons = mutableListOf<ButtonField>()
    val buttons: List<ButtonField> get() = _buttons

    internal fun register(field: ButtonField) {
        _buttons.add(field)
    }
}

operator fun ButtonField.provideDelegate(thisRef: ButtonGroup, property: KProperty<*>): ButtonField {
    val field = if (id.isEmpty()) withId(property.name) else this
    thisRef.register(field)
    return field
}

operator fun ButtonField.getValue(thisRef: ButtonGroup, property: KProperty<*>): ButtonField = this