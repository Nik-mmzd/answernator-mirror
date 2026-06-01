package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.Color
import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.toList

suspend fun Member.getColor(): Color? {
    return roles.toList().filter { it.color.rgb != 0 }.maxByOrNull { it.rawPosition }?.color
}
