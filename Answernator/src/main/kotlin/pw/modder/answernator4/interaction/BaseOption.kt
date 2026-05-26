package pw.modder.answernator4.interaction

import dev.kord.common.Locale
import dev.kord.common.entity.ApplicationCommandOptionType
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.rest.builder.interaction.AttachmentBuilder
import dev.kord.rest.builder.interaction.BooleanBuilder
import dev.kord.rest.builder.interaction.ChannelBuilder
import dev.kord.rest.builder.interaction.GroupCommandBuilder
import dev.kord.rest.builder.interaction.IntegerOptionBuilder
import dev.kord.rest.builder.interaction.MentionableBuilder
import dev.kord.rest.builder.interaction.NumberOptionBuilder
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.RoleBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import dev.kord.rest.builder.interaction.SubCommandBuilder
import dev.kord.rest.builder.interaction.UserBuilder

data class Choice<T>(
    val name: String,
    val nameLocalizations: Map<Locale, String>,
    val value: T
)

internal data class BaseOption<out T : Any>(
    override val type: ApplicationCommandOptionType,
    override val name: LocalizableString = LocalizableString.EMPTY,
    override val description: LocalizableString = LocalizableString.EMPTY,
    override val choices: List<Choice<*>> = emptyList(),
    override val channelTypes: List<ChannelType> = emptyList(),
    override val minLength: Int? = null,
    override val maxLength: Int? = null,
    override val minValue: Number? = null,
    override val maxValue: Number? = null,
    val valueExtractor: (OptionValue<*>?) -> T
) : OptionImpl<T>() {
    override val required: Boolean get() = true

    override fun name(name: LocalizableString) = copy(name = name)
    override fun description(description: LocalizableString) = copy(description = description)
    override fun choice(choice: Choice<*>) = copy(choices = choices + choice)
    override fun channelType(channelType: ChannelType) = copy(channelTypes = channelTypes + channelType)
    override fun minLength(minLength: Int) = copy(minLength = minLength)
    override fun maxLength(maxLength: Int) = copy(maxLength = maxLength)
    override fun minValue(minValue: Number) = copy(minValue = minValue)
    override fun maxValue(maxValue: Number) = copy(maxValue = maxValue)

    override fun withLocalizations(bundleName: String, nameKey: String, descriptionKey: String): BaseOption<T> = copy(
        name = name.copy(key = nameKey),
        description = description.copy(key = descriptionKey)
    )

    override fun extractValue(data: OptionValue<*>?): T {
        return valueExtractor(data)
    }

    override fun buildSpec(builder: OptionsBuilder, bundleName: String) {
        require(description != LocalizableString.EMPTY) {
            "Option '${name.key}' has no description. Discord requires a non-empty description for all options."
        }

        builder.required = required

        val (nameValue, nameLocs) = getAllLocalizations(bundleName, name.key)
        builder.name = nameValue
        builder.nameLocalizations?.putAll(nameLocs)

        val (descValue, descLocs) = getAllLocalizations(bundleName, description.key)
        builder.description = descValue
        builder.descriptionLocalizations?.putAll(descLocs)

        when (builder) {
            is StringChoiceBuilder -> {
                choices.forEach { choice ->
                    builder.choice(choice.name, choice.value as String) {
                        choice.nameLocalizations.forEach { (locale, translation) ->
                            nameLocalizations?.put(locale, translation)
                        }
                    }
                }
                minLength?.let { builder.minLength = it }
                maxLength?.let { builder.maxLength = it }
            }
            is IntegerOptionBuilder -> {
                choices.forEach { choice ->
                    builder.choice(choice.name, (choice.value as Number).toLong()) {
                        choice.nameLocalizations.forEach { (locale, translation) ->
                            nameLocalizations?.put(locale, translation)
                        }
                    }
                }
                minValue?.let { builder.minValue = it.toLong() }
                maxValue?.let { builder.maxValue = it.toLong() }
            }
            is NumberOptionBuilder -> {
                choices.forEach { choice ->
                    builder.choice(choice.name, (choice.value as Number).toDouble()) {
                        choice.nameLocalizations.forEach { (locale, translation) ->
                            nameLocalizations?.put(locale, translation)
                        }
                    }
                }
                minValue?.let { builder.minValue = it.toDouble() }
                maxValue?.let { builder.maxValue = it.toDouble() }
            }
            is ChannelBuilder -> {
                builder.channelTypes = channelTypes.toMutableList()
            }
            is AttachmentBuilder -> {}
            is BooleanBuilder -> {}
            is UserBuilder -> {}
            is RoleBuilder -> {}
            is MentionableBuilder -> {}
            is SubCommandBuilder -> {}
            is GroupCommandBuilder -> {}
        }
    }
}

fun string(): Option<String> = BaseOption(
    type = ApplicationCommandOptionType.String,
    valueExtractor = { option ->
        checkNotNull(option) { "Option required but missing" }
        option.value as String
    }
)

fun userId(): Option<Snowflake> = BaseOption(
    type = ApplicationCommandOptionType.User,
    valueExtractor = { option ->
        checkNotNull(option) { "Option required but missing" }
        Snowflake(option.value.toString())
    }
)

fun long(): Option<Long> = BaseOption(
    type = ApplicationCommandOptionType.Integer,
    valueExtractor = { option ->
        checkNotNull(option) { "Option required but missing" }
        option.value as Long
    }
)
