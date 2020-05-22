package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.util.DiskordInternals
import pw.modder.answernator.utils.auditLog.AuditLog


@OptIn(DiskordInternals::class)
@Suppress("UNUSED_PARAMETER")
suspend fun GuildClient.getAuditLog(useCustom: Boolean = true) = this.getRequest("/guilds/$guildId/audit-logs", AuditLog.serializer())