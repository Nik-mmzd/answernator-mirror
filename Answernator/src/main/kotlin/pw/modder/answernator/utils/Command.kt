package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.*
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.authorId
import kotlinx.serialization.UnstableDefault
import mu.KLogger
import mu.KotlinLogging
import java.util.*

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

enum class UserGroup {
    OWNER, ADMIN, ALL, PERMISSION
}

enum class ChannelTypes {
    GUILD, DIRECT, ALL
}

private val logger: KLogger = KotlinLogging.logger {}
@UnstableDefault
@DiskordDsl
interface Command {
    val name: String
    val lang: List<Locale> get() = listOf(Locale("en"), Locale("ru"))
    val userGroup: UserGroup get() = UserGroup.ALL
    val permission: Permission? get() = null
//    val timeout: Int get() = 0
    val channels: ChannelTypes get() = ChannelTypes.ALL

    suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed

    private fun check(message: Message): Boolean? {
        logger.debug { "checking command $name" }
        logger.debug { "checking channel type" }
        if (channels == ChannelTypes.DIRECT && message.partialMember != null) return false
        if (channels == ChannelTypes.GUILD && message.partialMember == null) return false

        logger.debug { "checking command is owner only" }
        if (userGroup == UserGroup.OWNER && message.authorId != GlobalConfig.get().author) return false
        if (userGroup == UserGroup.OWNER || userGroup == UserGroup.ALL) {
            logger.debug { "early exit because no permission checks is needed" }
            return true
        }

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
        val permissions = when (val member = guildClient?.getMember(message.authorId)) {
            null -> Permissions.NONE
            else -> member.computePermissions(guildClient)
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
}