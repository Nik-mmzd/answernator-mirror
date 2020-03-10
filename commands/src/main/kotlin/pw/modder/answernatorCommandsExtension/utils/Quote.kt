package pw.modder.answernatorCommandsExtension.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Quote(
    val id: Int,
    @SerialName("created_at") val createdAt: Long,
    val creator: Long,
    val text: String,
    val likesCount: Int
) {
    @Transient val creatorMention = creator.takeUnless { it == 1L }?.run { "<@$this>" } ?: "*неизвестен*"
}