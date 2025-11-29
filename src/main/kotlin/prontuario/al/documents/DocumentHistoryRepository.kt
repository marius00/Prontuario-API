package prontuario.al.documents

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.enum
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import org.springframework.stereotype.Repository
import prontuario.al.database.IBaseModel
import prontuario.al.ktorm.getDate
import prontuario.al.ktorm.getDateOrFail
import prontuario.al.ktorm.getOrFail
import java.time.Instant

@Repository
class DocumentHistoryRepository(private val database: Database) {
    fun list(): List<DocumentHistory> {
        return database
            .from(DocumentHistorys)
            .select()
            .map(DocumentHistorys::createEntity)
            .toList()
    }

    fun findById(id: Long): DocumentHistory? =
        database
            .from(DocumentHistorys)
            .select()
            .where { (DocumentHistorys.id eq id) }
            .map(DocumentHistorys::createEntity)
            .firstOrNull()

    fun saveRecord(record: DocumentHistory): DocumentHistory {
        val id = database.insertAndGenerateKey(DocumentHistorys) {
            set(it.documentId, record.documentId)
            set(it.action, record.action)
            set(it.sector, record.sector)
            set(it.description, record.description)
            set(it.createdAt, record.createdAt.epochSecond)
        } as Number

        return findById(id.toLong())!!
    }
}

enum class DocumentHistoryTypeEnum {
    CREATED,
    SENT,
    RECEIVED,
    REJECTED,
    UPDATED,
    REQUESTED,
    DELETED,
}
data class DocumentHistory(
    val id: Long?,
    val documentId: Long,
    val action: DocumentHistoryTypeEnum,
    val sector: String,
    val description: String?,
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant? = null,
    val deletedAt: Instant? = null,
) : IBaseModel {
    override fun getId(): Any? = id
}

object DocumentHistorys : BaseTable<DocumentHistory>("document_history") {
    val id = long("id").primaryKey()
    val documentId = long("document_id")
    val action = enum<DocumentHistoryTypeEnum>("action")
    val sector = varchar("sector")
    val description = varchar("description")
    val createdAt = long("created_at")
    val modifiedAt = long("updated_at")
    val deletedAt = long("deleted_at")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = DocumentHistory(
        id = row.getOrFail(id),
        action = row.getOrFail(action),
        documentId = row.getOrFail(documentId),
        description = row.getOrFail(description),
        sector = row.getOrFail(sector),
        createdAt = row.getDateOrFail(createdAt),
        modifiedAt = row.getDate(modifiedAt),
        deletedAt = row.getDate(deletedAt),
    )
}
