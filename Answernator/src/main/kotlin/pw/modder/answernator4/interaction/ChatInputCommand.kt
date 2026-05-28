package pw.modder.answernator4.interaction

import dev.kord.common.entity.ApplicationCommandType
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class ChatInputCommand : Command() {
    override val discordType = ApplicationCommandType.ChatInput

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

    operator fun <T> Option<T>.provideDelegate(thisRef: ChatInputCommand, property: KProperty<*>): Option<T> {
        val impl = this as OptionImpl<T>
        val named = if (impl.name == LocalizableString.EMPTY) impl.name(property.name) as OptionImpl<T> else impl
        registerOption(named as OptionImpl<*>)
        return named
    }

    operator fun <T> Option<T>.getValue(thisRef: ChatInputCommand, property: KProperty<*>): Option<T> = this

    override suspend fun register(kord: Kord) {
        kord.register(this)
    }

    abstract suspend fun ChatInputCommandInteractionCreateEvent.execute()
}