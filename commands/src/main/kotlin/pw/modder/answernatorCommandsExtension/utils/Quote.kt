package pw.modder.answernatorCommandsExtension.utils

data class Quote(
    val id: Int,
    val created_at: Long,
    val creator: Int,
    val text: String,
    val likesCount: String,
    val wasLiked: Boolean,
    val success: Boolean
)