package pw.modder.answernator4.interaction.modal

import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.rest.builder.component.LabelComponentBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.component.channelType
import dev.kord.rest.builder.component.option

data class SelectChoice(
    val label: String,
    val value: String,
    val description: String? = null,
    val default: Boolean = false,
)

internal fun SelectChoice.toDiscordSelectOption(): dev.kord.common.entity.DiscordSelectOption =
    dev.kord.rest.builder.component.SelectOptionBuilder(label, value).apply {
        this@toDiscordSelectOption.description?.let { description = it }
        if (this@toDiscordSelectOption.default) default = true
    }.build()

internal sealed class SelectFieldImpl<out T> : ModalFieldImpl<T>() {
    abstract override fun withId(id: String): SelectFieldImpl<T>
    abstract fun extractValues(submit: ModalSubmitInteraction): List<T>
}

internal data class StringSelectField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val choices: List<SelectChoice>,
    val placeholder: String? = null,
) : SelectFieldImpl<String>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.stringSelect(id) {
            this@StringSelectField.placeholder?.let { placeholder = it }
            this@StringSelectField.choices.forEach { c ->
                option(c.label, c.value) {
                    c.description?.let { description = it }
                    if (c.default) default = true
                }
            }
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        submit.stringSelects[id]?.values?.isNotEmpty() == true

    override fun extractValues(submit: ModalSubmitInteraction): List<String> =
        submit.stringSelects[id]?.values.orEmpty()

    override fun extractValue(submit: ModalSubmitInteraction): String =
        extractValues(submit).firstOrNull().orEmpty()
}

internal class MultipleField<T>(
    val wrapped: SelectFieldImpl<T>,
    val allowedValues: IntRange,
) : ModalFieldImpl<List<T>>() {
    override val id: String get() = wrapped.id
    override val label: String get() = wrapped.label
    override val description: String? get() = wrapped.description

    override fun withId(id: String): ModalFieldImpl<List<T>> =
        MultipleField(wrapped.withId(id), allowedValues)

    override fun buildIn(builder: LabelComponentBuilder) {
        wrapped.buildIn(builder)
        (builder.component as SelectMenuBuilder).allowedValues = allowedValues
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean = wrapped.hasValue(submit)

    override fun extractValue(submit: ModalSubmitInteraction): List<T> =
        wrapped.extractValues(submit)
}

fun stringSelect(
    label: String,
    choices: List<SelectChoice>,
    description: String? = null,
    placeholder: String? = null,
): ModalField<String> = StringSelectField(
    label = label,
    description = description,
    placeholder = placeholder,
    choices = choices,
)

internal data class UserSelectField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val placeholder: String? = null,
    val defaultUsers: List<Snowflake> = emptyList(),
) : SelectFieldImpl<Snowflake>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.userSelect(id) {
            this@UserSelectField.placeholder?.let { placeholder = it }
            defaultUsers.addAll(this@UserSelectField.defaultUsers)
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        submit.userSelects[id]?.valueIds?.isNotEmpty() == true

    override fun extractValues(submit: ModalSubmitInteraction): List<Snowflake> =
        submit.userSelects[id]?.valueIds.orEmpty()

    override fun extractValue(submit: ModalSubmitInteraction): Snowflake =
        extractValues(submit).firstOrNull() ?: Snowflake(0u)
}

internal data class RoleSelectField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val placeholder: String? = null,
    val defaultRoles: List<Snowflake> = emptyList(),
) : SelectFieldImpl<Snowflake>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.roleSelect(id) {
            this@RoleSelectField.placeholder?.let { placeholder = it }
            defaultRoles.addAll(this@RoleSelectField.defaultRoles)
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        submit.roleSelects[id]?.valueIds?.isNotEmpty() == true

    override fun extractValues(submit: ModalSubmitInteraction): List<Snowflake> =
        submit.roleSelects[id]?.valueIds.orEmpty()

    override fun extractValue(submit: ModalSubmitInteraction): Snowflake =
        extractValues(submit).firstOrNull() ?: Snowflake(0u)
}

internal data class MentionableSelectField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val placeholder: String? = null,
) : SelectFieldImpl<Snowflake>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.mentionableSelect(id) {
            this@MentionableSelectField.placeholder?.let { placeholder = it }
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        submit.mentionableSelects[id]?.valueIds?.isNotEmpty() == true

    override fun extractValues(submit: ModalSubmitInteraction): List<Snowflake> =
        submit.mentionableSelects[id]?.valueIds.orEmpty()

    override fun extractValue(submit: ModalSubmitInteraction): Snowflake =
        extractValues(submit).firstOrNull() ?: Snowflake(0u)
}

internal data class ChannelSelectField(
    override val id: String = "",
    override val label: String,
    override val description: String? = null,
    val placeholder: String? = null,
    val channelTypes: List<ChannelType> = emptyList(),
    val defaultChannels: List<Snowflake> = emptyList(),
) : SelectFieldImpl<Snowflake>() {
    override fun withId(id: String) = copy(id = id)

    override fun buildIn(builder: LabelComponentBuilder) {
        builder.channelSelect(id) {
            this@ChannelSelectField.placeholder?.let { placeholder = it }
            this@ChannelSelectField.channelTypes.forEach { channelType(it) }
            defaultChannels.addAll(this@ChannelSelectField.defaultChannels)
        }
    }

    override fun hasValue(submit: ModalSubmitInteraction): Boolean =
        submit.channelSelects[id]?.valueIds?.isNotEmpty() == true

    override fun extractValues(submit: ModalSubmitInteraction): List<Snowflake> =
        submit.channelSelects[id]?.valueIds.orEmpty()

    override fun extractValue(submit: ModalSubmitInteraction): Snowflake =
        extractValues(submit).firstOrNull() ?: Snowflake(0u)
}

fun userSelect(
    label: String,
    description: String? = null,
    placeholder: String? = null,
    defaultUsers: List<Snowflake> = emptyList(),
): ModalField<Snowflake> = UserSelectField(
    label = label,
    description = description,
    placeholder = placeholder,
    defaultUsers = defaultUsers,
)

fun roleSelect(
    label: String,
    description: String? = null,
    placeholder: String? = null,
    defaultRoles: List<Snowflake> = emptyList(),
): ModalField<Snowflake> = RoleSelectField(
    label = label,
    description = description,
    placeholder = placeholder,
    defaultRoles = defaultRoles,
)

fun mentionableSelect(
    label: String,
    description: String? = null,
    placeholder: String? = null,
): ModalField<Snowflake> = MentionableSelectField(
    label = label,
    description = description,
    placeholder = placeholder,
)

fun channelSelect(
    label: String,
    description: String? = null,
    placeholder: String? = null,
    channelTypes: List<ChannelType> = emptyList(),
    defaultChannels: List<Snowflake> = emptyList(),
): ModalField<Snowflake> = ChannelSelectField(
    label = label,
    description = description,
    placeholder = placeholder,
    channelTypes = channelTypes,
    defaultChannels = defaultChannels,
)

fun <T> ModalField<T>.multiple(allowedValues: IntRange = 1..25): ModalField<List<T>> {
    val impl = this as? SelectFieldImpl<T>
        ?: error("multiple() is only supported on select fields (got ${this::class.simpleName})")
    return MultipleField(impl, allowedValues)
}