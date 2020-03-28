package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.util.DiskordInternals
import pw.modder.answernator.utils.auditLog.AuditLog

@Suppress("UNUSED_PARAMETER")
@UseExperimental(DiskordInternals::class)
suspend fun GuildClient.getAuditLog(useCustom: Boolean = true) = getRequest("/guilds/$guildId/audit-logs", AuditLog.serializer())