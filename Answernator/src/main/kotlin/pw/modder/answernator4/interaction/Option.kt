package pw.modder.answernator4.interaction

import dev.kord.common.Locale
import dev.kord.common.entity.ApplicationCommandOptionType
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.rest.builder.interaction.OptionsBuilder

sealed class Option<out T> {
    abstract val type: ApplicationCommandOptionType
    abstract val required: Boolean

    abstract val name: LocalizableString
    abstract fun name(name: LocalizableString): Option<T>

    abstract val description: LocalizableString
    abstract fun description(description: LocalizableString): Option<T>

    abstract fun buildSpec(builder: OptionsBuilder, bundleName: String)
}

internal sealed class OptionImpl<out T> : Option<T>() {
    abstract override fun name(name: LocalizableString): OptionImpl<T>
    abstract override fun description(description: LocalizableString): OptionImpl<T>

    abstract val choices: List<Choice<*>>
    abstract fun choice(choice: Choice<*>): OptionImpl<T>

    abstract val channelTypes: List<ChannelType>
    abstract fun channelType(channelType: ChannelType): OptionImpl<T>

    abstract val minLength: Int?
    abstract fun minLength(minLength: Int): OptionImpl<T>

    abstract val maxLength: Int?
    abstract fun maxLength(maxLength: Int): OptionImpl<T>

    abstract val minValue: Number?
    abstract fun minValue(minValue: Number): OptionImpl<T>

    abstract val maxValue: Number?
    abstract fun maxValue(maxValue: Number): OptionImpl<T>

    abstract fun withLocalizations(bundleName: String, nameKey: String, descriptionKey: String): OptionImpl<T>

    abstract fun extractValue(data: OptionValue<*>?): T

    abstract override fun buildSpec(builder: OptionsBuilder, bundleName: String)

    override fun toString(): String {
        return "Option(name=$name, type=$type, required=$required)"
    }
}

fun <T> Option<T>.name(name: String): Option<T> {
    return name(name.asLocaleKey())
}

fun <T> Option<T>.description(description: String): Option<T> {
    return description(description.asLocaleKey())
}

fun <T> Option<T>.localized(bundleName: String, nameKey: String? = null, descriptionKey: String? = null): Option<T> {
    val impl = this as OptionImpl<T>
    return impl.withLocalizations(bundleName, nameKey ?: impl.name.key, descriptionKey ?: impl.description.key)
}

fun Option<String>.choice(
    value: String,
    nameKey: String,
    bundleName: String
): Option<String> {
    val (name, localizations) = getAllLocalizations(bundleName, nameKey)
    return (this as OptionImpl<String>).choice(Choice(name, localizations, value))
}

fun Option<Long>.choice(
    value: Long,
    nameKey: String,
    bundleName: String
): Option<Long> {
    val (name, localizations) = getAllLocalizations(bundleName, nameKey)
    return (this as OptionImpl<Long>).choice(Choice(name, localizations, value))
}

fun Option<Int>.choice(
    value: Int,
    nameKey: String,
    bundleName: String
): Option<Int> {
    val (name, localizations) = getAllLocalizations(bundleName, nameKey)
    return (this as OptionImpl<Int>).choice(Choice(name, localizations, value.toLong()))
}

fun Option<String>.choice(
    value: String,
    name: String,
    vararg localizations: Pair<Locale, String>
): Option<String> = choice(value, name, localizations.toMap())

fun Option<String>.choice(
    value: String,
    name: String,
    localizations: Map<Locale, String> = emptyMap()
): Option<String> = (this as OptionImpl<String>).choice(Choice(name, localizations, value))

fun Option<Short>.choice(
    value: Short,
    nameKey: String,
    bundleName: String
): Option<Short> {
    val (name, localizations) = getAllLocalizations(bundleName, nameKey)
    return (this as OptionImpl<Short>).choice(Choice(name, localizations, value.toLong()))
}

fun Option<Byte>.choice(
    value: Byte,
    nameKey: String,
    bundleName: String
): Option<Byte> {
    val (name, localizations) = getAllLocalizations(bundleName, nameKey)
    return (this as OptionImpl<Byte>).choice(Choice(name, localizations, value.toLong()))
}

fun Option<Double>.choice(
    value: Double,
    nameKey: String,
    bundleName: String
): Option<Double> {
    val (name, localizations) = getAllLocalizations(bundleName, nameKey)
    return (this as OptionImpl<Double>).choice(Choice(name, localizations, value))
}

fun Option<Float>.choice(
    value: Float,
    nameKey: String,
    bundleName: String
): Option<Float> {
    val (name, localizations) = getAllLocalizations(bundleName, nameKey)
    return (this as OptionImpl<Float>).choice(Choice(name, localizations, value.toDouble()))
}

fun Option<Long>.choice(
    value: Long,
    name: String,
    vararg localizations: Pair<Locale, String>
): Option<Long> = choice(value, name, localizations.toMap())

fun Option<Long>.choice(
    value: Long,
    name: String,
    localizations: Map<Locale, String> = emptyMap()
): Option<Long> = (this as OptionImpl<Long>).choice(Choice(name, localizations, value))

fun Option<Long>.minValue(minValue: Long): Option<Long> =
    (this as OptionImpl<Long>).minValue(minValue)

fun Option<Long>.maxValue(maxValue: Long): Option<Long> =
    (this as OptionImpl<Long>).maxValue(maxValue)

fun Option<Long>.coerceValue(minValue: Long, maxValue: Long): Option<Long> =
    minValue(minValue).maxValue(maxValue)

//fun Option<Long>.coerceValue(range: ClosedRange<Long>): Option<Long> =
//    coerceValue(range.start, range.endInclusive)

fun Option<Int>.choice(
    value: Int,
    name: String,
    vararg localizations: Pair<Locale, String>
): Option<Int> = choice(value, name, localizations.toMap())

fun Option<Int>.choice(
    value: Int,
    name: String,
    localizations: Map<Locale, String> = emptyMap()
): Option<Int> = (this as OptionImpl<Int>).choice(Choice(name, localizations, value.toLong()))

fun Option<Int>.minValue(minValue: Int): Option<Int> =
    (this as OptionImpl<Int>).minValue(minValue.toLong())

fun Option<Int>.maxValue(maxValue: Int): Option<Int> =
    (this as OptionImpl<Int>).maxValue(maxValue.toLong())

fun Option<Int>.coerceValue(minValue: Int, maxValue: Int): Option<Int> = minValue(minValue).maxValue(maxValue)
//fun Option<Int>.coerceValue(range: ClosedRange<Int>): Option<Int> = coerceValue(range.start, range.endInclusive)


fun Option<Short>.choice(
    value: Short,
    name: String,
    vararg localizations: Pair<Locale, String>
): Option<Short> = choice(value, name, localizations.toMap())

fun Option<Short>.choice(
    value: Short,
    name: String,
    localizations: Map<Locale, String> = emptyMap()
): Option<Short> =
    (this as OptionImpl<Short>).choice(Choice(name, localizations, value.toLong()))

fun Option<Short>.minValue(minValue: Short): Option<Short> =
    (this as OptionImpl<Short>).minValue(minValue.toLong())

fun Option<Short>.maxValue(maxValue: Short): Option<Short> =
    (this as OptionImpl<Short>).maxValue(maxValue.toLong())

fun Option<Short>.coerceValue(minValue: Short, maxValue: Short): Option<Short> =
    minValue(minValue).maxValue(maxValue)

//fun Option<Short>.coerceValue(range: ClosedRange<Short>): Option<Short> =
//    coerceValue(range.start, range.endInclusive)

fun Option<Byte>.choice(
    value: Byte,
    name: String,
    vararg localizations: Pair<Locale, String>
): Option<Byte> = choice(value, name, localizations.toMap())

fun Option<Byte>.choice(
    value: Byte,
    name: String,
    localizations: Map<Locale, String> = emptyMap()
): Option<Byte> = (this as OptionImpl<Byte>).choice(Choice(name, localizations, value.toLong()))

fun Option<Byte>.minValue(minValue: Byte): Option<Byte> =
    (this as OptionImpl<Byte>).minValue(minValue.toLong())

fun Option<Byte>.maxValue(maxValue: Byte): Option<Byte> =
    (this as OptionImpl<Byte>).maxValue(maxValue.toLong())

fun Option<Byte>.coerceValue(minValue: Byte, maxValue: Byte): Option<Byte> =
    minValue(minValue).maxValue(maxValue)

//fun Option<Byte>.coerceValue(range: ClosedRange<Byte>): Option<Byte> =
//    coerceValue(range.start, range.endInclusive)

fun Option<Double>.choice(
    value: Double,
    name: String,
    vararg localizations: Pair<Locale, String>
): Option<Double> = choice(value, name, localizations.toMap())

fun Option<Double>.choice(
    value: Double,
    name: String,
    localizations: Map<Locale, String> = emptyMap()
): Option<Double> = (this as OptionImpl<Double>).choice(Choice(name, localizations, value))

fun Option<Double>.minValue(minValue: Double): Option<Double> =
    (this as OptionImpl<Double>).minValue(minValue)

fun Option<Double>.maxValue(maxValue: Double): Option<Double> =
    (this as OptionImpl<Double>).maxValue(maxValue)

fun Option<Double>.coerceValue(minValue: Double, maxValue: Double): Option<Double> =
    minValue(minValue).maxValue(maxValue)

//fun Option<Double>.coerceValue(range: ClosedRange<Double>): Option<Double> =
//    coerceValue(range.start, range.endInclusive)

fun Option<Float>.choice(
    value: Float,
    name: String,
    vararg localizations: Pair<Locale, String>
): Option<Float> = choice(value, name, localizations.toMap())

fun Option<Float>.choice(
    value: Float,
    name: String,
    localizations: Map<Locale, String> = emptyMap()
): Option<Float> =
    (this as OptionImpl<Float>).choice(Choice(name, localizations, value.toDouble()))

fun Option<Float>.minValue(minValue: Float): Option<Float> =
    (this as OptionImpl<Float>).minValue(minValue.toDouble())

fun Option<Float>.maxValue(maxValue: Float): Option<Float> =
    (this as OptionImpl<Float>).maxValue(maxValue.toDouble())

fun Option<Float>.coerceValue(minValue: Float, maxValue: Float): Option<Float> =
    minValue(minValue).maxValue(maxValue)

//fun Option<Float>.coerceValue(range: ClosedRange<Float>): Option<Float> =
//    coerceValue(range.start, range.endInclusive)

fun Option<Channel>.channelTypes(vararg types: ChannelType): Option<Channel> {
    return types.fold(this as OptionImpl<Channel>) { option, type ->
        option.channelType(type)
    }
}

fun Option<String>.minLength(minLength: Int): Option<String> = (this as OptionImpl<String>).minLength(minLength)
fun Option<String>.maxLength(maxLength: Int): Option<String> = (this as OptionImpl<String>).maxLength(maxLength)