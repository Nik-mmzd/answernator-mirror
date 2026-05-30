package pw.modder.answernator4.db.tables

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import pw.modder.answernator4.db.integerBoolean

object AntiSpamConfigs : IdTable<Long>() {
    // id is the guild snowflake, supplied externally — not autoincremented.
    override val id: Column<EntityID<Long>> = long("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val isEnabled = integerBoolean("is_enabled")
    val isMrBeastEnabled = integerBoolean("filter_mrbeast")

    val warningText = varchar("warning_text", 255)
    val muteText = varchar("mute_text", 255)
    val banText = varchar("ban_text", 255)

    val warningThreshold = integer("warning_threshold")
    val muteThreshold = integer("mute_threshold")
    val banRepeats = integer("ban_threshold")

    init {
        index(false, isEnabled)
        index(false, isMrBeastEnabled)
    }
}

class AntiSpamConfig(id: EntityID<Long>): LongEntity(id) {
    companion object : LongEntityClass<AntiSpamConfig>(AntiSpamConfigs)

    var isEnabled by AntiSpamConfigs.isEnabled
    var isMrBeastEnabled by AntiSpamConfigs.isMrBeastEnabled

    var warningText by AntiSpamConfigs.warningText
    var muteText by AntiSpamConfigs.muteText
    var banText by AntiSpamConfigs.banText

    var warningThreshold by AntiSpamConfigs.warningThreshold
    var muteThreshold by AntiSpamConfigs.muteThreshold
    var banRepeats by AntiSpamConfigs.banRepeats
}
