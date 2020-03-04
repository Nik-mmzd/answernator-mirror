package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault
import java.io.File
import java.net.URLClassLoader
import java.util.*

@UnstableDefault
object CommandList {
    var commands: List<Command> = listOf()

    fun load() {
        val classLoader = URLClassLoader(
            File(".").resolve("commands").also { if (!it.exists()) it.mkdirs() }.listFiles { file: File ->
                file.isFile && file.extension.equals("jar", ignoreCase = true)
            }?.map { it.toURI().toURL() }?.toTypedArray() ?: arrayOf()
        )

        commands = ServiceLoader.load(Command::class.java, classLoader).toList()
        println("Loaded ${commands.size} commands")
    }
}