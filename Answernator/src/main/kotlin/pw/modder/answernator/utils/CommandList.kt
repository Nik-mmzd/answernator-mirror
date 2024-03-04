package pw.modder.answernator.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URLClassLoader
import java.util.*

private val logger = KotlinLogging.logger {  }
object CommandList {
    var commands: List<Command> = listOf()
    var modules: List<ModuleInfoProvider> = listOf()

    fun load() {
        val classLoader = URLClassLoader(
            File(".").resolve("commands").also { if (!it.exists()) it.mkdirs() }.listFiles { file: File ->
                file.isFile && file.extension.equals("jar", ignoreCase = true)
            }?.map { it.toURI().toURL() }?.toTypedArray() ?: arrayOf()
        )

        commands = ServiceLoader.load(Command::class.java, classLoader).filterNot { it.name in Globals.config.commandsBlacklist }
        modules = ServiceLoader.load(ModuleInfoProvider::class.java, classLoader).toList()
        logger.info { "Loaded ${commands.size} commands, ${modules.size} modules" }
    }

    fun findCommand(name: String, ignoreCase: Boolean = true): Command? {
        return commands.singleOrNull { it.name.equals(name, ignoreCase) }
    }

    fun findCommand(name: String, ignoreCase: Boolean = true, channelType: Command.ChannelTypes): Command? {
        return commands.singleOrNull { it.name.equals(name, ignoreCase) && channelType in it.channels }
    }
}