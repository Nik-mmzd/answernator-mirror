package pw.modder.answernator.utils

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.entity.Message
import mu.KLogger
import mu.KotlinLogging
import pw.modder.answernator.utils.extensions.kord.authorId
import java.util.*

private val logger: KLogger = KotlinLogging.logger {}
interface Command {
    val name: String
    val userGroup: UserGroup get() = UserGroup.ALL
    val permission: Permission? get() = null
//    val timeout: Int get() = 0
    val channels: EnumSet<ChannelTypes> get() = EnumSet.of(ChannelTypes.DIRECT, ChannelTypes.GUILD)
    val cmdType: CommandGroup get() = CommandGroup.OTHER
    val requiredPermission: Permission? get() = null

    suspend fun check(message: Message): Boolean {
        logger.debug { "checking command $name" }
        logger.debug { "checking command is owner only" }
        if (userGroup == UserGroup.OWNER && message.authorId != Globals.config.author) return false
        if (userGroup == UserGroup.OWNER || userGroup == UserGroup.ALL) {
            logger.debug { "early exit because no permission checks is needed" }
            return true
        }

        if (message.authorId == Globals.config.author
            && userGroup == UserGroup.ADMIN
            && channels.contains(ChannelTypes.DIRECT)) return true

        logger.debug { "getting permissions" }
        val perms = (message.getAuthorAsMember()?.getPermissions() ?: Permissions())

        if (userGroup == UserGroup.ADMIN && !perms.contains(Permission.Administrator)) return false
        if (userGroup == UserGroup.PERMISSION && !perms.contains(permission ?: return false)) return false

        return true
    }

    fun getHelp(locale: Locale): String? {
        return null
    }
    fun getDescription(locale: Locale): String? {
        return null
    }

    suspend fun action(message: Message, args: List<String>, locale: Locale)

    enum class UserGroup {
        OWNER, ADMIN, ALL, PERMISSION
    }

    enum class ChannelTypes {
        GUILD, DIRECT
    }

    enum class CommandGroup {
        DEBUG, OWNER, ADMIN, MODER, USER, FUN, OTHER
    }
}