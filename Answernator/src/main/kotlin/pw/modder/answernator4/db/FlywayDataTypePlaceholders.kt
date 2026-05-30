package pw.modder.answernator4.db

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.vendors.*
import org.jetbrains.exposed.v1.datetime.*
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.*
import java.util.*

object FlywayDataTypePlaceholders : FlywayPlaceholdersProvider {
    override fun providePlaceholders(): Map<String, String> {
        val manager = TransactionManager.current()
        val dialect = manager.db.dialect
        val dataTypeProvider = dialect.dataTypeProvider

        val map = mapping.entries.associateTo(mutableMapOf()) { (name, mapper) ->
            "$PREFIX:$name" to mapper(dataTypeProvider)
        }

        expressionMapping.entries.associateTo(map) { (name, expression) ->
            val queryBuilder = QueryBuilder(false)
            expression.toQueryBuilder(queryBuilder)
            "$PREFIX:$name" to queryBuilder.toString()
        }

        (manager.connection as? JdbcConnectionImpl)?.let { connection ->
            map["q"] = connection.connection.metaData.identifierQuoteString
        }

        map["PK_FOR_AUTOINC"] = if (dialect is SQLiteDialect) "" else "PRIMARY KEY"

        return Collections.unmodifiableMap(map)
    }

    private const val PREFIX = "datatype"

    private val postgresDataTypeProvider = PostgreSQLDialect().dataTypeProvider

    private val mapping = mapOf(
        "blob" to DataTypeProvider::blobType,
        "boolean" to DataTypeProvider::booleanType,
        "byte" to DataTypeProvider::byteType,
        "dateTime" to DataTypeProvider::dateTimeType,
        "double" to DataTypeProvider::doubleType,
        "false" to { it.booleanToStatementString(false) },
        "float" to DataTypeProvider::floatType,
        "integer" to DataTypeProvider::integerType,
        "integerAutoinc" to DataTypeProvider::integerAutoincType,
        "long" to DataTypeProvider::longType,
        "longAutoinc" to DataTypeProvider::longAutoincType,
        "short" to DataTypeProvider::shortType,
        "text" to DataTypeProvider::textType,
        "time" to DataTypeProvider::timeType,
        "true" to { it.booleanToStatementString(true) },
        "ubyte" to DataTypeProvider::ubyteType,
        "uinteger" to DataTypeProvider::uintegerType,
        "ulong" to DataTypeProvider::ulongType,
        "ushort" to DataTypeProvider::ushortType,
    ) + (1..256).associate { length ->
        "binary($length)" to { provider ->
            if (provider == postgresDataTypeProvider) {
                provider.binaryType()
            } else {
                provider.binaryType(length)
            }
        }
    }

    private val expressionMapping = mapOf<String, Expression<*>>(
        "current_timestamp" to CurrentTimestamp,
    )
}
