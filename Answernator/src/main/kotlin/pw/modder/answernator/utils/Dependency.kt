package pw.modder.answernator.utils

data class Dependency(val group: String, val name: String, val version: String) {
    companion object {
        fun fromString(artifact: String): Dependency {
            val s = artifact.split(':', limit = 3)
            return Dependency(s[0], s[1], s[2])
        }
    }
}