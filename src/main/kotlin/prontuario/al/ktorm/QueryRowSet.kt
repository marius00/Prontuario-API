package prontuario.al.ktorm

import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.Column
import java.time.Instant

fun <C : Any> QueryRowSet.getOrFail(column: Column<C>): C = get(column) ?: throw Exception()

fun QueryRowSet.getDate(column: Column<Long>): Instant? = get(column)?.let { Instant.ofEpochSecond(it) }

fun QueryRowSet.getDateOrFail(column: Column<Long>): Instant = getDate(column) ?: throw Exception()
