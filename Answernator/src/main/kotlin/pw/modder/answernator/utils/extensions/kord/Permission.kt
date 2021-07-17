package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Permission

val Permission.name get() = this::class.simpleName ?: "Unknown"