package prontuario.al.documents

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.enum
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import org.springframework.stereotype.Repository
import prontuario.al.auth.UserId
import prontuario.al.database.IBaseModel
import prontuario.al.generated.types.DocumentTypeEnum
import prontuario.al.generated.types.Sector
import prontuario.al.ktorm.getDate
import prontuario.al.ktorm.getDateOrFail
import prontuario.al.ktorm.getOrFail
import java.time.Instant

@Repository
class DocumentRepository(private val database: Database) {
    fun list(): List<Document> {
        return database
            .from(Documents)
            .select()
            .map(Documents::createEntity)
            .toList()
    }

    fun findById(id: DocumentId): Document? =
        database
            .from(Documents)
            .select()
            .where { (Documents.id eq id.value) }
            .map(Documents::createEntity)
            .firstOrNull()

    fun update(record: Document): Document {
        database.update(Documents) {
            set(it.number, record.number)
            set(it.name, record.name)
            set(it.observations, record.observations)
            set(it.type, record.type)
            set(it.sector, record.sector.name)
            set(it.modifiedAt, Instant.now().epochSecond)
            where {
                it.id eq record.id!!.value
            }
        }

        return findById(record.id!!)!!
    }

    fun saveRecord(record: Document): Document {
        val id = database.insertAndGenerateKey(Documents) {
            set(it.number, record.number)
            set(it.name, record.name)
            set(it.observations, record.observations)
            set(it.type, record.type)
            set(it.sector, record.sector.name)
            set(it.createdAt, record.createdAt.epochSecond)
        } as Number

        return findById(DocumentId(id.toLong()))!!
    }
}

@JvmInline
value class DocumentId(
    val value: Long,
)


data class Document(
    val id: DocumentId?,
    val number: String,
    val name: String,
    val observations: String?,
    val type: DocumentTypeEnum,
    var sector: Sector,
    var createdBy: UserId,
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant? = null,
    val deletedAt: Instant? = null,
) : IBaseModel {
    override fun getId(): Any? = id
}

object Documents : BaseTable<Document>("document") {
    val id = long("id").primaryKey()
    val number = varchar("number")
    val name = varchar("name")
    val observations = varchar("observations")
    val sector = varchar("sector")
    val userId = long("user_id")
    val type = enum<DocumentTypeEnum>("type")
    val createdAt = long("created_at")
    val modifiedAt = long("updated_at")
    val deletedAt = long("deleted_at")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = Document(
        id = DocumentId(row.getOrFail(id)),
        name = row.getOrFail(name),
        number = row.getOrFail(number),
        observations = row.getOrFail(observations),
        sector = Sector(row.getOrFail(sector), null),
        type = row.getOrFail(type),
        createdBy = UserId(row.getOrFail(userId)),
        createdAt = row.getDateOrFail(createdAt),
        modifiedAt = row.getDate(modifiedAt),
        deletedAt = row.getDate(deletedAt),
    )
}
