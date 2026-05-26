package pw.modder.answernator4.interaction

import dev.kord.common.entity.ApplicationCommandOptionType
import dev.kord.core.Kord
import dev.kord.rest.builder.interaction.*
import kotlin.reflect.KProperty

fun ChatInputCreateBuilder.option(option: Option<*>, bundleName: String) {
    val impl = option as OptionImpl<*>
    when (impl.type) {
        ApplicationCommandOptionType.String -> string(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        ApplicationCommandOptionType.Integer -> integer(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        ApplicationCommandOptionType.Boolean -> boolean(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        ApplicationCommandOptionType.User -> user(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        ApplicationCommandOptionType.Channel -> channel(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        ApplicationCommandOptionType.Role -> role(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        ApplicationCommandOptionType.Mentionable -> mentionable(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        ApplicationCommandOptionType.Number -> number(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        ApplicationCommandOptionType.Attachment -> attachment(impl.name.key, impl.description.key) { impl.buildSpec(this, bundleName) }
        else -> error("Unsupported option type: ${impl.type}")
    }
}

operator fun <T> Option<T>.provideDelegate(thisRef: ChatInputCommand, property: KProperty<*>): Option<T> {
    val impl = this as OptionImpl<T>
    val named = if (impl.name == LocalizableString.EMPTY) impl.name(property.name) as OptionImpl<T> else impl
    thisRef.registerOption(named as OptionImpl<*>)
    return named
}

operator fun <T> Option<T>.getValue(thisRef: ChatInputCommand, property: KProperty<*>): Option<T> = this

suspend fun Kord.register(command: ChatInputCommand) {
    val bName = command.bundleName
    val dKey = command.description.key

    val (desc, descLocs) = getAllLocalizations(bName, dKey)

    createGlobalChatInputCommand(command.name, desc) {
        if (desc.isNotEmpty()) {
            descriptionLocalizations?.putAll(descLocs)
        }

        val (nameValue, nameLocs) = getAllLocalizations(bName, command.name)
        name = nameValue
        nameLocalizations?.putAll(nameLocs)

        defaultMemberPermissions = command.defaultMemberPermissions
        dmPermission = command.dmPermission

        command.options.forEach { option(it, bName) }
    }
}

suspend fun Kord.registerUser(command: UserCommand) {
    val bName = command.bundleName

    createGlobalUserCommand(command.name) {
        val (nameValue, nameLocs) = getAllLocalizations(bName, command.name)
        name = nameValue
        nameLocalizations?.putAll(nameLocs)

        defaultMemberPermissions = command.defaultMemberPermissions
        dmPermission = command.dmPermission
    }
}

suspend fun Kord.registerMessage(command: MessageCommand) {
    val bName = command.bundleName

    createGlobalMessageCommand(command.name) {
        val (nameValue, nameLocs) = getAllLocalizations(bName, command.name)
        name = nameValue
        nameLocalizations?.putAll(nameLocs)

        defaultMemberPermissions = command.defaultMemberPermissions
        dmPermission = command.dmPermission
    }
}
