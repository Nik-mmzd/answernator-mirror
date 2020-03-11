package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault

interface ModuleInfoProvider {
    val name: String
    val version: String
        get() {
            return javaClass.classLoader.getResourceAsStream("module.txt").use { it.reader().readText() }
        }
}

class ModuleInfo: ModuleInfoProvider {
    override val name = "Base"
    @UnstableDefault
    override val version = Globals.getDependencyVersion("pw.modder", "answernator")
}