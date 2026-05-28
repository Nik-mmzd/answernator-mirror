package pw.modder.answernator4.command

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.json.request.BulkDeleteRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import pw.modder.answernator.utils.extensions.kord.authorId
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.Option
import pw.modder.answernator4.interaction.default
import pw.modder.answernator4.interaction.description
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.interaction.long
import pw.modder.answernator4.interaction.maxValue
import pw.modder.answernator4.interaction.minValue
import pw.modder.answernator4.interaction.optional
import pw.modder.answernator4.interaction.string
import pw.modder.answernator4.interaction.userId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val logger = KotlinLogging.logger {}
class Clean : ChatInputCommand() {
    override val name = "clean"
    override val bundleName = "v4.clean"
    override val defaultMemberPermissions = Permissions(Permission.ManageMessages)
    override val dmPermission = false

    val limit: Option<Long> by long().description("clean.limit")
    val minutes: Option<Long?> by long().description("clean.minutes").minValue(1).maxValue(1440).optional()
    val user1: Option<Snowflake?> by userId().description("clean.user").optional()
    val user2: Option<Snowflake?> by userId().description("clean.user").optional()
    val user3: Option<Snowflake?> by userId().description("clean.user").optional()
    val user4: Option<Snowflake?> by userId().description("clean.user").optional()
    val reason: Option<String?> by string().description("clean.reason").optional()

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferEphemeralResponse()
        val limit by option(limit)
        val minutes by option(minutes)
        val minTimestamp = Clock.System.now().minus(minutes?.minutes ?: 1.days)
        val reason by option(reason)
        val usersFilter = buildSet {
            val user1 by option(user1)
            if (user1 != null) add(user1)
            val user2 by option(user2)
            if (user2 != null) add(user2)
            val user3 by option(user3)
            if (user3 != null) add(user3)
            val user4 by option(user4)
            if (user4 != null) add(user4)
        }

        if (limit !in 1..1000) {
            reply.respond { content = bundle.l("command.clean.invalid_limit").format(limit) }
            return
        }
        if (minutes != null && minutes!! !in 1..1440) {
            reply.respond { content = bundle.l("command.clean.invalid_minutes").format(minutes) }
            return
        }

        var errored = false
        var removed = 0
        @OptIn(ExperimentalCoroutinesApi::class) // for chunked()
        interaction.channel.messages
            .filter { usersFilter.isEmpty() || it.authorId in usersFilter }
            .takeWhile { it.timestamp >= minTimestamp }
            .map { it.id }
            .take(limit.toInt())
            // discord allows up to 100 messages per bulk delete request
            .chunked(100)
            .collect {
                try {
                    interaction.channel.kord.rest.channel.bulkDelete(interaction.channel.id, BulkDeleteRequest(it), reason)
                    removed += it.size
                } catch (e: Exception) {
                    logger.warn(e) { "An error occurred while bulk deleting message" }
                    errored = true
                }
            }

        val messageText = if (errored)
            bundle.l("command.clean.error").format(removed)
        else
            bundle.l("command.clean.success").format(removed)

        reply.respond { content = messageText }
    }
}
