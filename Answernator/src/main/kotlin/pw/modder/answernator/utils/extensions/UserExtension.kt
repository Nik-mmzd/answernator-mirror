package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.User
import pw.modder.answernator.utils.Utils

val User.createdAt: Long get() = Utils.snowflakeCreatedAt(id)