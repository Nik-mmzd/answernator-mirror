package pw.modder.answernator4.db

import org.jetbrains.exposed.v1.core.Table

fun Table.integerBoolean(name: String, checkConstraintName: String? = null) = integer(name, checkConstraintName).transform(
    wrap = { it > 0 },
    unwrap = { if (it) 1 else 0 }
)
