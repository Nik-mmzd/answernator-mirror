package pw.modder.answernator4.db

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.Table.Dual.transform

fun Column<Long>.asSnowflake() = this.transform(SnowflakeTransformer)

object SnowflakeTransformer : ColumnTransformer<Long, Snowflake> {
    override fun wrap(value: Long): Snowflake {
        return Snowflake(value)
    }

    override fun unwrap(value: Snowflake): Long {
        return value.value.toLong()
    }
}
