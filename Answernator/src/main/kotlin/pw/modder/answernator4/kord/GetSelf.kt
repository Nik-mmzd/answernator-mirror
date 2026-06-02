package pw.modder.answernator4.kord

import dev.kord.core.Kord
import dev.kord.core.supplier.EntitySupplyStrategy

suspend fun Kord.getSelfCached() = getSelf(EntitySupplyStrategy.cacheWithCachingRestFallback)
