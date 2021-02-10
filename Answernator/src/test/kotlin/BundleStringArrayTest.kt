import pw.modder.answernator.utils.UTF8Control
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

fun main() {
    println("1")
    val texts = CommandLocaleBundle("mute", Locale("ru"))

    println(texts.getString("reason.0"))
    println(texts.getRandomString("reason"))
    println(texts.getRandomString("reason"))
    println(texts.getRandomString("reason"))
    println(texts.getRandomString("reason"))
    println(texts.getRandomString("reason"))
}