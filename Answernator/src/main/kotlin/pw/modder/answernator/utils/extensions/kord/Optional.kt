package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.optional.Optional

fun <T> Optional<T?>.orElse(value: T): T {
    return this.value ?: value
}

inline fun <T> Optional<T?>.orElse(block: () -> T): T {
    return this.value ?: block()
}

fun <T> Optional<T?>.hasValue(): Boolean {
    return value != null
}

inline fun <T> Optional<T?>.ifHasValue(block: (T) -> Unit) {
    this.value?.apply(block)
}