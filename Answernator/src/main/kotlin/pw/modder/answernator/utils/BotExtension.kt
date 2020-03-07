package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import pw.modder.answernator.cache.RolesCache.getRolesCached
import pw.modder.answernator.cache.GuildMemberRolesCache.getMemberRolesCached
import pw.modder.answernator.cache.GuildOwnerCache.getOwnerCached
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
object BotGlobalLocale {
    private val locales = GlobalConfig.get().langs.associateBy({ Locale(it) }, {
        ResourceBundle.clearCache(javaClass.classLoader)
        ResourceBundle.getBundle("locale.botGlobal", Locale(it), javaClass.classLoader, UTF8Control())
    })

    fun getString(locale: Locale, str: String): String {
        logger.debug { "getting string \"$str\" for locale ${locale.toLanguageTag()}" }
        return try {
            locales[locale]?.getString("bot.$str") ?: "bot.$str"
        } catch (_: MissingResourceException) {
            "bot.$str"
        }
    }

    fun formatString(locale: Locale, str: String, vararg arguments: Any?): String {
        return String.format(getString(locale, str), args = *arguments)
    }
}

private val logger = KotlinLogging.logger {}
@UnstableDefault
@DiskordDsl
fun Bot.loadCommandService() {
    val config = GlobalConfig.get()

    messageCreated { message: Message ->
        if (message.content.isEmpty()) return@messageCreated
        logger.debug { "received message, message text: ${message.content}" }
        if (!message.content.startsWith(config.prefix)) return@messageCreated

        val locale = message.guildId?.run { GuildConfigs.get(this).locale } ?: config.locale
        val channelType =if (message.guildId == null) Command.ChannelTypes.DIRECT else Command.ChannelTypes.GUILD
        CommandList.commands.singleOrNull { command ->
            logger.debug { "probing command ${command.name}, searching for ${message.words.first()}" }
            message.words.first().equals("${config.prefix}${command.name}", true) && channelType in command.channels
        }?.run {
            logger.debug { "found command $name, running" }
            if (check(message, message.guildId?.run { clientStore.guilds[this] })) {
                val reply = try {
                    action(clientStore, message, locale)
                } catch (_: NotImplementedError) {
                    message.reply(BotGlobalLocale.formatString(locale, "notImplemented", "${config.prefix}$name"))
                    return@run
                } catch (e: Exception) {
                    logger.error(e) { "got error while running command" }
                    message.reply(BotGlobalLocale.getString(locale, "error"))
                    return@run
                }

                message.reply(reply.text, reply.embed())
                return@run
            }

            message.reply(BotGlobalLocale.getString(locale, "noPerms"))
        }
    }
}

@UnstableDefault
@DiskordDsl
fun Bot.greetingsService() {
    userJoinedGuild {
        val config = GuildConfigs.get(it.guildId)
        if (config.greetNewUsers && config.greetingsChannel.isNotEmpty()) {
            clientStore.channels[config.greetingsChannel].sendMessage(String.format(
                config.greetingText,
                it.nickname ?: it.user?.username ?: "new user",
                clientStore.guilds[it.guildId].get().name
            ))
        }
    }
}

suspend fun GuildClient.computePermissions(memberId: String): Permissions {
    if (getOwnerCached() == memberId) return Permissions.ALL


    var permissions = Permissions.NONE
    val memberRoles = getMemberRolesCached(memberId)
    val roles = getRolesCached().filter { it.id in memberRoles }

    roles.forEach { role ->
        if (role.permissions.contains(Permission.ADMINISTRATOR)) return Permissions.ALL
        permissions += permissions + role.permissions
    }

    return permissions
}
