package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.GuildClients
import com.jessecorbett.diskord.util.authorId
import mu.KLogger
import mu.KotlinLogging
import pw.modder.answernator.utils.extensions.computePermissions
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

private val logger: KLogger = KotlinLogging.logger {}
interface Command {
    val name: String
    val userGroup: UserGroup get() = UserGroup.ALL
    val permission: Permission? get() = null
//    val timeout: Int get() = 0
    val channels: EnumSet<ChannelTypes> get() = EnumSet.of(ChannelTypes.DIRECT, ChannelTypes.GUILD)
    val cmdType: CommandGroup get() = CommandGroup.OTHER
    val requiredPermission: Permission? get() = null

    suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed

    private fun check(message: Message): Boolean? {
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

        return null
    }

    private fun check(permissions: Permissions): Boolean {
        logger.debug { "checking permissions" }
        if (userGroup == UserGroup.ADMIN && !permissions.contains(Permission.ADMINISTRATOR)) return false
        if (userGroup == UserGroup.PERMISSION && !permissions.contains(permission ?: return false)) return false

        return true
    }

    suspend fun check(message: Message, guildClients: GuildClients): Boolean {
        check(message)?.run { return this }

        logger.debug { "getting permissions" }
        val permissions = when (val gid = message.guildId) {
            null -> Permissions.NONE
            else -> message.partialMember?.computePermissions(guildClients[gid], message.authorId) ?: Permissions.NONE
        }

        return check(permissions)
    }

    fun check(message: Message, permissions: Permissions): Boolean {
        check(message)?.run { return this }

        return check(permissions)
    }

    fun textMessage(message: String): CombinedMessageEmbed = dslmessage { text = message }

    fun getHelp(locale: Locale): String? {
        return null
    }
    fun getDescription(locale: Locale): String? {
        return null
    }

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