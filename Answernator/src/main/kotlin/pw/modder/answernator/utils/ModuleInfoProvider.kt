package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault

interface ModuleInfoProvider {
    val name: String
    val version: String
        get() {
            return try {
                javaClass.classLoader.getResourceAsStream("module.$name.txt").use { it.reader().readText() }
            } catch (_: Exception) {
                "unknown"
            }
        }
}

class ModuleInfo: ModuleInfoProvider {
    override val name = "Base"
    @UnstableDefault
    override val version = Globals.getDependencyVersion("pw.modder", "Answernator")
}