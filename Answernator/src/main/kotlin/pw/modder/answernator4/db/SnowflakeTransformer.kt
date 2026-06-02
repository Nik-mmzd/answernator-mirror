package pw.modder.answernator4.db

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer

// `transform` is a member of Table that mutates the receiver table's columns (replaceColumn). It
// must run with the column's *own* table as receiver — importing it from Table.Dual instead piles
// every transformed column onto the shared Dual singleton, throwing DuplicateColumnException as
// soon as two columns share a name (e.g. "user_id").
fun Column<Long>.asSnowflake(): Column<Snowflake> = with(table) {
    this@asSnowflake.transform(SnowflakeTransformer)
}

object SnowflakeTransformer : ColumnTransformer<Long, Snowflake> {
    override fun wrap(value: Long): Snowflake {
        return Snowflake(value)
    }

    override fun unwrap(value: Snowflake): Long {
        return value.value.toLong()
    }
}
