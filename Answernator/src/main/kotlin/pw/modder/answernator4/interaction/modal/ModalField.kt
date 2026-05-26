package pw.modder.answernator4.interaction.modal

import dev.kord.common.entity.TextInputStyle
import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.rest.builder.component.CheckboxGroupBuilder
import dev.kord.rest.builder.component.LabelComponentBuilder
import dev.kord.rest.builder.component.RadioGroupBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.component.TextInputBuilder

sealed interface ModalElement

data class TextDisplay(val content: String) : ModalElement

sealed class ModalField<out T> : ModalElement {
    abstract val id: String
    abstract val label: String
    abstract val description: String?

    internal abstract fun withId(id: String): ModalField<T>
    internal abstract fun buildIn(builder: LabelComponentBuilder)
    internal abstract fun hasValue(submit: ModalSubmitInteraction): Boolean
    internal abstract fun extractValue(submit: ModalSubmitInteraction): T
}

internal sealed class ModalFieldImpl<out T> : ModalField<T>() {
    abstract override fun withId(id: String): ModalFieldImpl<T>
}

internal data class TextInputField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val style: TextInputStyle = TextInputStyle.Short,
    val placeholder: String? = null,
    val allowedLength: IntRange? = null,
    val defaultValue: String? = null,
) : ModalFieldImpl<String>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.textInput(style, id) {
            required = true
            this@TextInputField.placeholder?.let { placeholder = it }
            this@TextInputField.allowedLength?.let { allowedLength = it }
            this@TextInputField.defaultValue?.let { value = it }
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        !submit.textInputs[id]?.value.isNullOrEmpty()

    override fun extractValue(submit: ModalSubmitInteraction): String =
        submit.textInputs[id]?.value.orEmpty()
}

internal sealed class WrapperField<R, out T>(
    val wrapped: ModalFieldImpl<R>,
) : ModalFieldImpl<T>() {
    override val id: String get() = wrapped.id
    override val label: String get() = wrapped.label
    override val description: String? get() = wrapped.description
}

internal class OptionalField<T : Any>(
    wrapped: ModalFieldImpl<T>,
) : WrapperField<T, T?>(wrapped) {
    override fun withId(id: String): ModalFieldImpl<T?> = OptionalField(wrapped.withId(id))

    override fun buildIn(builder: LabelComponentBuilder) {
        wrapped.buildIn(builder)
        when (val c = builder.component) {
            is TextInputBuilder -> c.required = false
            is SelectMenuBuilder -> c.allowedValues = 0..c.allowedValues.endInclusive
            is RadioGroupBuilder -> c.required = false
            is CheckboxGroupBuilder -> {
                c.required = false
                c.minValues = 0
            }
            else -> {}
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean = wrapped.hasValue(submit)

    override fun extractValue(submit: ModalSubmitInteraction): T? =
        if (wrapped.hasValue(submit)) wrapped.extractValue(submit) else null
}

fun textField(
    label: String,
    description: String? = null,
    style: TextInputStyle = TextInputStyle.Short,
    placeholder: String? = null,
    allowedLength: IntRange? = null,
    defaultValue: String? = null,
): ModalField<String> = TextInputField(
    label = label,
    description = description,
    style = style,
    placeholder = placeholder,
    allowedLength = allowedLength,
    defaultValue = defaultValue,
)

@Suppress("UNCHECKED_CAST")
fun <T : Any> ModalField<T>.optional(): ModalField<T?> = OptionalField(this as ModalFieldImpl<T>)