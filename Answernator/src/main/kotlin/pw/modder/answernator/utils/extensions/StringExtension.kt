package pw.modder.answernator.utils.extensions

fun String.removeGraves(): String {
    return replace("`", "")
}