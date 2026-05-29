package pw.modder.answernator.utils

@Deprecated("Switch to v4")
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

@Deprecated("Switch to v4")
class ModuleInfo: ModuleInfoProvider {
    override val name = "Base"
    override val version = Globals.getDependencyVersion("pw.modder", "Answernator")
}
