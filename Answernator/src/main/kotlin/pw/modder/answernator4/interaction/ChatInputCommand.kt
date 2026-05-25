package pw.modder.answernator4.interaction

import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import kotlin.properties.ReadOnlyProperty

abstract class ChatInputCommand : Command() {
    open val description: LocalizableString = LocalizableString.EMPTY

    fun <T> ChatInputCommandInteractionCreateEvent.option(option: Option<T>): ReadOnlyProperty<Any?, T> {
        val resolvedName = getAllLocalizations(bundleName, option.name.key).first
        return ReadOnlyProperty { _, _ ->
            val commandOption = interaction.command.options[resolvedName]
            (option as OptionImpl<T>).extractValue(commandOption)
        }
    }

    private val _options = mutableListOf<OptionImpl<*>>()
    val options: List<Option<*>> get() = _options

    internal fun registerOption(option: OptionImpl<*>) {
        _options.add(option)
    }

    override suspend fun register(kord: Kord) {
        kord.register(this)
    }

    abstract suspend fun ChatInputCommandInteractionCreateEvent.execute()
}