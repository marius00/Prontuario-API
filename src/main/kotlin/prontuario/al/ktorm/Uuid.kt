package prontuario.al.ktorm

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.nio.ByteBuffer
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.*

fun BaseTable<*>.uuid(name: String): Column<UUID> = registerColumn(name, UuidSqlType)

object UuidSqlType : SqlType<UUID>(Types.BINARY, "uuid") {
    override fun doSetParameter(
        ps: PreparedStatement,
        index: Int,
        parameter: UUID,
    ) {
        val bytes = ByteBuffer.wrap(ByteArray(16))

        bytes.putLong(parameter.mostSignificantBits)
        bytes.putLong(parameter.leastSignificantBits)

        ps.setObject(index, bytes.array())
    }

    override fun doGetResult(
        rs: ResultSet,
        index: Int,
    ): UUID? {
        val bytes = rs.getObject(index) as? ByteArray

        return bytes
            ?.let { ByteBuffer.wrap(it) }
            ?.let {
                val high = it.getLong()
                val low = it.getLong()

                UUID(high, low)
            }
    }
}
