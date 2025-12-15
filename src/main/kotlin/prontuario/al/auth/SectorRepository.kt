package prontuario.al.auth

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import org.springframework.stereotype.Repository
import prontuario.al.auth.Users.id
import prontuario.al.database.IBaseModel
import prontuario.al.ktorm.getDate
import prontuario.al.ktorm.getDateOrFail
import prontuario.al.ktorm.getOrFail
import java.time.Instant

@Repository
class SectorRepository(
    private val database: Database,
) {
    fun list(): List<Sector> =
        database
            .from(Sectors)
            .select()
            .where { Sectors.deletedAt.isNull() }
            .map(Sectors::createEntity)
            .toList()

    fun list(includeDeleted: Boolean = false): List<Sector> {
        val query = database
            .from(Sectors)
            .select()

        return if (includeDeleted) {
            query
        } else {
            query.where { Sectors.deletedAt.isNull() }
        }
            .map(Sectors::createEntity)
            .toList()
    }

    fun saveRecord(record: Sector) {
        val id = database.insert(Sectors) {
            set(it.name, record.name)
            set(it.createdAt, record.createdAt.epochSecond)
        }
    }

    fun reActivate(record: prontuario.al.auth.Sector) {
        database.update(Sectors) {
            set(it.deletedAt, null)
            where {
                it.name eq record.name
            }
        }
    }

    fun deactivate(record: prontuario.al.auth.Sector) {
        database.update(Sectors) {
            set(it.deletedAt, Instant.now().epochSecond)
            where {
                it.name eq record.name
            }
        }
    }
}

data class Sector(
    val name: String,
    val code: String?,
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant? = null,
    val deletedAt: Instant? = null,
) : IBaseModel {
    override fun getId(): Any? = id
}

object Sectors : BaseTable<Sector>("sector") {
    val name = varchar("name")
    val code = varchar("code")
    val createdAt = long("created_at")
    val modifiedAt = long("modified_at")
    val deletedAt = long("deleted_at")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = Sector(
        name = row.getOrFail(name),
        code = row[code],
        createdAt = row.getDateOrFail(createdAt),
        modifiedAt = row.getDate(modifiedAt),
        deletedAt = row.getDate(deletedAt),
    )
}
