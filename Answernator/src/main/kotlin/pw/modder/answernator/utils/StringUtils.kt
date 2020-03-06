package pw.modder.answernator.utils

fun String.removeGraves(): String {
    return replace("`", "")
}