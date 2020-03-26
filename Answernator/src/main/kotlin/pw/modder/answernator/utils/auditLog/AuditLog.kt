package pw.modder.answernator.utils.auditLog

import com.jessecorbett.diskord.api.model.User
import com.jessecorbett.diskord.api.model.Webhook
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuditLog(
    @SerialName("webhooks") val webhooks: List<Webhook>,
    @SerialName("users") val users: List<User>,
    @SerialName("audit_log_entries") val entries: List<AuditLogEntry>
)

@Serializable
data class AuditLogEntry(
    @SerialName("id") val id: String,
    @SerialName("target_id") val targetId: String?,
    @SerialName("changes") val changes: List<AuditLogChange> = emptyList(),
    @SerialName("user_id") val userId: String,
    @SerialName("action_type") val actionType: Int,
    @SerialName("options") val optionalData: JsonElement? = null,
    @SerialName("reason") val reason: String? = null
)

// TODO: Make super dynamic and all https://discordapp.com/developers/docs/resources/audit-log#audit-log-change-object
@Serializable
data class AuditLogChange(
    @SerialName("new_value") val newValue: JsonElement? = null,
    @SerialName("old_value") val oldValue: JsonElement? = null,
    @SerialName("key") val key: String
)
