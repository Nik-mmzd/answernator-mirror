import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

fun main() {
    println("1")
    val texts = CommandLocaleBundle("mute", Locale("ru"))

    println(texts.get("reason.0"))
    println(texts.random("reason"))
    println(texts.random("reason"))
    println(texts.random("reason"))
    println(texts.random("reason"))
    println(texts.random("reason"))
}