package pw.modder.answernator4.interaction

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

private val logger = KotlinLogging.logger {}

object InteractionCommandList {
    var commands: List<Command> = listOf()
        private set

    fun load() {
        val classLoader = URLClassLoader(
            File(".").resolve("commands")
                .also { if (!it.exists()) it.mkdirs() }
                .listFiles { file -> file.isFile && file.extension.equals("jar", ignoreCase = true) }
                ?.map { it.toURI().toURL() }?.toTypedArray() ?: arrayOf()
        )
        commands = ServiceLoader.load(Command::class.java, classLoader).toList()
        logger.info { "Loaded ${commands.size} interaction commands" }
    }
}