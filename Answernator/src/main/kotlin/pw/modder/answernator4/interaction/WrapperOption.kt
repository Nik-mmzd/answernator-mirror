package pw.modder.answernator4.interaction

import dev.kord.common.entity.ApplicationCommandOptionType
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.rest.builder.interaction.OptionsBuilder

private sealed class WrapperOption<R, out T> : OptionImpl<T>() {
    abstract val option: OptionImpl<R>

    override val type: ApplicationCommandOptionType
        get() = option.type
    override val name: LocalizableString
        get() = option.name
    override val description: LocalizableString
        get() = option.description
    override val choices: List<Choice<*>>
        get() = option.choices
    override val channelTypes: List<ChannelType>
        get() = option.channelTypes
    override val required: Boolean
        get() = option.required
    override val minLength: Int?
        get() = option.minLength
    override val maxLength: Int?
        get() = option.maxLength
    override val minValue: Number?
        get() = option.minValue
    override val maxValue: Number?
        get() = option.maxValue

    protected abstract fun wrap(option: OptionImpl<R>): OptionImpl<T>

    override fun name(name: LocalizableString) = wrap(option.name(name))
    override fun description(description: LocalizableString) = wrap(option.description(description))
    override fun choice(choice: Choice<*>) = wrap(option.choice(choice))
    override fun channelType(channelType: ChannelType) = wrap(option.channelType(channelType))
    override fun minLength(minLength: Int) = wrap(option.minLength(minLength))
    override fun maxLength(maxLength: Int) = wrap(option.maxLength(maxLength))
    override fun minValue(minValue: Number) = wrap(option.minValue(minValue))
    override fun maxValue(maxValue: Number) = wrap(option.maxValue(maxValue))

    override fun withLocalizations(bundleName: String, nameKey: String, descriptionKey: String): OptionImpl<T> =
        wrap(option.withLocalizations(bundleName, nameKey, descriptionKey))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as WrapperOption<*, *>
        return option == other.option
    }

    override fun hashCode(): Int {
        return option.hashCode()
    }

    override fun buildSpec(builder: OptionsBuilder, bundleName: String) {
        option.buildSpec(builder, bundleName)
    }
}

private class OptionalOption<T : Any>(
    override val option: OptionImpl<T>,
) : WrapperOption<T, T?>() {
    override val required: Boolean
        get() = false

    override fun extractValue(data: OptionValue<*>?): T? {
        if (data == null) {
            return null
        }
        return option.extractValue(data)
    }

    override fun buildSpec(builder: OptionsBuilder, bundleName: String) {
        super.buildSpec(builder, bundleName)
        builder.required = false
    }

    override fun wrap(option: OptionImpl<T>): OptionImpl<T?> = OptionalOption(option)
}

fun <T : Any> Option<T>.optional(): Option<T?> = OptionalOption(this as OptionImpl<T>)

private class DefaultOption<T : Any>(
    override val option: OptionImpl<T>,
    private val defaultValue: T,
) : WrapperOption<T, T>() {
    override val required: Boolean
        get() = false

    override fun extractValue(data: OptionValue<*>?): T {
        if (data == null) {
            return defaultValue
        }
        return option.extractValue(data)
    }

    override fun buildSpec(builder: OptionsBuilder, bundleName: String) {
        super.buildSpec(builder, bundleName)
        builder.required = false
    }

    override fun wrap(option: OptionImpl<T>): OptionImpl<T> = DefaultOption(option, defaultValue)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as DefaultOption<*>
        return option == other.option && defaultValue == other.defaultValue
    }

    override fun hashCode(): Int {
        var result = option.hashCode()
        result = 31 * result + defaultValue.hashCode()
        return result
    }
}

fun <T : Any> Option<T>.default(value: T): Option<T> = DefaultOption(this as OptionImpl<T>, value)

private class TransformerOption<T, R>(
    override val option: OptionImpl<R>,
    private val transformer: (OptionValue<*>?, () -> R) -> T
) : WrapperOption<R, T>() {
    override fun wrap(option: OptionImpl<R>): OptionImpl<T> = TransformerOption(option, transformer)

    override fun extractValue(data: OptionValue<*>?): T = transformer(data) {
        option.extractValue(data)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TransformerOption<*, *>

        if (option != other.option) return false
        if (transformer != other.transformer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = option.hashCode()
        result = 31 * result + transformer.hashCode()
        return result
    }
}

fun <T, R> Option<T>.map(
    transform: (OptionValue<*>?, oldValue: () -> T) -> R
): Option<R> = TransformerOption(this as OptionImpl<T>, transform)