package pw.modder.answernator4.db

interface FlywayPlaceholdersProvider {
    fun providePlaceholders(): Map<String, String>
}
