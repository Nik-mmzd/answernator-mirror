package pw.modder.answernator4.interaction

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.LinkOption
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.isRegularFile
import kotlin.io.path.reader

internal class CommandRegistryCache(path: String) {
    private val path = Paths.get(path)
    private val props = Properties()
    private val logger = KotlinLogging.logger {}

    fun load() {
        if (!path.isRegularFile()) return
        try {
            path.bufferedReader(Charsets.UTF_8).use { props.load(it) }
            logger.debug { "Loaded command registry cache (${props.size} entries)" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load command registry cache, all commands will be re-registered" }
            props.clear()
        }
    }

    fun isUnchanged(key: String, hash: String): Boolean = props.getProperty(key) == hash

    fun update(key: String, hash: String) {
        props.setProperty(key, hash)
    }

    /** Drops cached hashes for commands that are no longer loaded (e.g. after a module set change). */
    fun retainOnly(keys: Set<String>) {
        val stale = props.stringPropertyNames() - keys
        if (stale.isEmpty()) return
        stale.forEach { props.remove(it) }
        logger.info { "Removed ${stale.size} stale command hash cache entr${if (stale.size == 1) "y" else "ies"}: $stale" }
    }

    fun save() {
        try {
            path.bufferedWriter(Charsets.UTF_8).use { props.store(it, null) }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to save command registry cache" }
        }
    }
}
