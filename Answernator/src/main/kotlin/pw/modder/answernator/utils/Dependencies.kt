package pw.modder.answernator.utils

object Dependencies {
    private val list: List<Dependency> = Utils.loadDependenciesList()

    fun get(): List<Dependency> {
        return list
    }

    fun getVersion(group: String, name: String): String {
        return list.single { it.group == group && it.name == name }.version
    }
}