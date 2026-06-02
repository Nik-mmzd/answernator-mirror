package pw.modder.answernator4.interaction

import java.security.MessageDigest

internal fun Command.specHash(): String {
    val sb = StringBuilder()

    val (nameValue, nameLocs) = getAllLocalizations(bundleName, name)
    sb.appendLine("name=$nameValue")
    nameLocs.entries.sortedBy { it.key.language }.forEach { (locale, v) ->
        sb.appendLine("name[${locale.language}]=$v")
    }
    sb.appendLine("type=${discordType.value}")

    if (this is ChatInputCommand) {
        val (desc, descLocs) = getAllLocalizations(bundleName, "$name.description")
        sb.appendLine("desc=$desc")
        descLocs.entries.sortedBy { it.key.language }.forEach { (locale, v) ->
            sb.appendLine("desc[${locale.language}]=$v")
        }
        options.forEachIndexed { i, opt -> appendOptionSpec(sb, "o[$i]", opt, bundleName) }
    }

    sb.appendLine("perms=${defaultMemberPermissions?.code?.value}")
    sb.appendLine("dm=$dmPermission")
    guildIds.map { it.toString() }.sorted().forEachIndexed { i, id -> sb.appendLine("g[$i]=$id") }

    return sha256(sb.toString())
}

private fun appendOptionSpec(sb: StringBuilder, prefix: String, option: Option<*>, bundleName: String) {
    val impl = option as OptionImpl<*>
    val (nameValue, nameLocs) = getAllLocalizations(bundleName, impl.name.key)
    val (descValue, descLocs) = getAllLocalizations(bundleName, impl.description.key)

    sb.appendLine("$prefix.name=$nameValue")
    nameLocs.entries.sortedBy { it.key.language }.forEach { (locale, v) ->
        sb.appendLine("$prefix.name[${locale.language}]=$v")
    }
    sb.appendLine("$prefix.desc=$descValue")
    descLocs.entries.sortedBy { it.key.language }.forEach { (locale, v) ->
        sb.appendLine("$prefix.desc[${locale.language}]=$v")
    }
    sb.appendLine("$prefix.type=${impl.type.type}")
    sb.appendLine("$prefix.required=${impl.required}")
    impl.channelTypes.map { it.value.toString() }.sorted().forEachIndexed { j, t ->
        sb.appendLine("$prefix.ct[$j]=$t")
    }
    impl.minValue?.let { sb.appendLine("$prefix.min=$it") }
    impl.maxValue?.let { sb.appendLine("$prefix.max=$it") }
    impl.minLength?.let { sb.appendLine("$prefix.minL=$it") }
    impl.maxLength?.let { sb.appendLine("$prefix.maxL=$it") }
    impl.choices.forEachIndexed { j, c ->
        sb.appendLine("$prefix.c[$j].name=${c.name}")
        c.nameLocalizations.entries.sortedBy { it.key.language }.forEach { (locale, v) ->
            sb.appendLine("$prefix.c[$j].name[${locale.language}]=$v")
        }
        sb.appendLine("$prefix.c[$j].value=${c.value}")
    }
}

private fun sha256(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }