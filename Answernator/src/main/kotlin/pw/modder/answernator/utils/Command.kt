package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.*
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.authorId
import kotlinx.serialization.UnstableDefault
import mu.KLogger
import mu.KotlinLogging
import java.util.*
import pw.modder.answernator.utils.extensions.computePermissions

//   return Command:new("help string", beta) -- beta boolean is optional
//    :langs("lang1", "lang2") -- optional
//    :groupUser() :groupAdmin() :groupOwner() :groupPerm("permission") -- optional
//    :guildWhitelist("guildID_1", "guildID_2") :guildBlacklist("guildID_1", "guildID_2") -- optional
//    :memberWhitelist("memberID_1", "memberID_2") ::memberBlacklist("memberID_1", "memberID_2") -- optional
//    :split( number ) -- optional
//    :timeout( number ) -- ptional
//    :beta ( boolean ) -- optional
//    :typeAll() :typeServer() :typePM() -- optional
//    :code(
//      function( message, arg )
//        -- body...
//      end
//    ) -- yep :code must be LAST
private val logger: KLogger = KotlinLogging.logger {}
@UnstableDefault
interface Command {
    val name: String
    val userGroup: UserGroup get() = UserGroup.ALL
    val permission: Permission? get() = null
//    val timeout: Int get() = 0
    val channels: EnumSet<ChannelTypes> get() = EnumSet.of(ChannelTypes.DIRECT, ChannelTypes.GUILD)

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

    suspend fun check(message: Message, guildClient: GuildClient? = null): Boolean {
        check(message)?.run { return this }

        logger.debug { "getting permissions" }
        val permissions = when (guildClient) {
            null -> Permissions.NONE
            else -> guildClient.computePermissions(message.authorId)
        }

        return check(permissions)
    }

    fun check(message: Message, permissions: Permissions): Boolean {
        check(message)?.run { return this }

        return check(permissions)
    }

    fun textMessage(message: String): CombinedMessageEmbed {
        return com.jessecorbett.diskord.dsl.message { text = message }
    }

    fun getHelp(locale: Locale): String? {
        return null
    }

    enum class UserGroup {
        OWNER, ADMIN, ALL, PERMISSION
    }

    enum class ChannelTypes {
        GUILD, DIRECT
    }
}