package pw.modder.answernator.utils.extensions

@Deprecated("Switch to v4")
fun Iterable<String>.joinToStrings(limit: Int = 2000, separator: CharSequence = " "): List<String> {
    val result = mutableListOf<String>()
    val builder = StringBuilder()
    for (line in this) {
        if (builder.length + line.length + separator.length > limit) {
            result.add(builder.toString())
            builder.setLength(0)
        }
        if (builder.isNotEmpty()) builder.append(separator)
        builder.append(line)
    }
    if (builder.isNotEmpty()) result.add(builder.toString())
    return result
}
