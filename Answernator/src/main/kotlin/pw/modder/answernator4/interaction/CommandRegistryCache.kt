package pw.modder.answernator4.interaction

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.Properties

internal class CommandRegistryCache(path: String) {
    private val file = File(path)
    private val props = Properties()
    private val logger = KotlinLogging.logger {}

    fun load() {
        if (!file.exists()) return
        try {
            file.bufferedReader().use { props.load(it) }
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

    fun save() {
        try {
            file.bufferedWriter().use { props.store(it, null) }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to save command registry cache" }
        }
    }
}