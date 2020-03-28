package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.api.model.User
import com.jessecorbett.diskord.dsl.Bot

private var me: User? = null
suspend fun Bot.getMe(): User {
    return me ?: clientStore.discord.getUser("@me").also { me = it }
}

suspend fun Bot.isMe(userId: String): Boolean = getMe().id == userId
suspend fun Bot.isMe(user: User): Boolean = isMe(user.id)