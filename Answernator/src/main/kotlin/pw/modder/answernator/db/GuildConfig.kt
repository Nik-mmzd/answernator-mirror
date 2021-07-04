package pw.modder.answernator.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GuildConfig(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GuildConfig>(GuildConfigs)

    var guildId by GuildConfigs.guildId
    var lang by GuildConfigs.lang
    var greetNewUsers by GuildConfigs.greetNewUsers
    var greetingText by GuildConfigs.greetingText
    var greetingsChannel by GuildConfigs.greetingsChannel
    var muteRole by GuildConfigs.muteRole
    var defaultRole by GuildConfigs.defaultRole
    var antiSpam by GuildConfigs.antiSpam
}