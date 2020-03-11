package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
import java.io.File
import java.net.URLClassLoader
import java.util.*

private val logger = KotlinLogging.logger {  }
@UnstableDefault
object CommandList {
    var commands: List<Command> = listOf()
    var modules: List<ModuleInfoProvider> = listOf()

    fun load() {
        val classLoader = URLClassLoader(
            File(".").resolve("commands").also { if (!it.exists()) it.mkdirs() }.listFiles { file: File ->
                file.isFile && file.extension.equals("jar", ignoreCase = true)
            }?.map { it.toURI().toURL() }?.toTypedArray() ?: arrayOf()
        )

        commands = ServiceLoader.load(Command::class.java, classLoader).toList()
        modules = ServiceLoader.load(ModuleInfoProvider::class.java, classLoader).toList()
        logger.info { "Loaded ${commands.size} commands, ${modules.size} modules" }
    }
}