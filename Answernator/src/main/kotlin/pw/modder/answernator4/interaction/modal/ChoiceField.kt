package pw.modder.answernator4.interaction.modal

import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.rest.builder.component.LabelComponentBuilder

internal data class RadioGroupField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val choices: List<SelectChoice>,
) : ModalFieldImpl<String>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.radioGroup(id) {
            required = true
            options = this@RadioGroupField.choices.map { it.toDiscordSelectOption() }
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        submit.radioGroups[id]?.value != null

    override fun extractValue(submit: ModalSubmitInteraction): String =
        submit.radioGroups[id]?.value.orEmpty()
}

internal data class CheckboxGroupField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val choices: List<SelectChoice>,
    val allowedValues: IntRange? = null,
) : ModalFieldImpl<List<String>>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.checkboxGroup(id) {
            required = true
            options = this@CheckboxGroupField.choices.map { it.toDiscordSelectOption() }
            this@CheckboxGroupField.allowedValues?.let {
                minValues = it.start
                maxValues = it.endInclusive
            }
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        submit.checkboxGroups[id]?.values?.isNotEmpty() == true

    override fun extractValue(submit: ModalSubmitInteraction): List<String> =
        submit.checkboxGroups[id]?.values.orEmpty()
}

internal data class CheckboxField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val default: Boolean = false,
) : ModalFieldImpl<Boolean>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.checkbox(id) {
            if (this@CheckboxField.default) default = true
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        id in submit.checkboxes

    override fun extractValue(submit: ModalSubmitInteraction): Boolean =
        submit.checkboxes[id]?.value ?: default
}

fun radioGroup(
    label: String,
    choices: List<SelectChoice>,
    description: String? = null,
): ModalField<String> = RadioGroupField(
    label = label,
    description = description,
    choices = choices,
)

fun checkboxGroup(
    label: String,
    choices: List<SelectChoice>,
    allowedValues: IntRange? = null,
    description: String? = null,
): ModalField<List<String>> = CheckboxGroupField(
    label = label,
    description = description,
    choices = choices,
    allowedValues = allowedValues,
)

fun checkbox(
    label: String,
    default: Boolean = false,
    description: String? = null,
): ModalField<Boolean> = CheckboxField(
    label = label,
    description = description,
    default = default,
)