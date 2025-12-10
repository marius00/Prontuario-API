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
import prontuario.al.auth.Users as UserTable

@Repository
class DocumentRepository(
    private val database: Database,
    private val documentHistoryRepository: DocumentHistoryRepository,
) {
    fun list(): List<Document> =
        database
            .from(Documents)
            .leftJoin(UserTable, on = Documents.userId eq UserTable.id)
            .select()
            .map(Documents::createEntity)
            .toList()
            .let { loadDocumentsWithHistory(it) }

    fun exists(documentNumber: String): Boolean =
        database
            .from(Documents)
            .select()
            .where { Documents.number eq documentNumber }
            .iterator()
            .hasNext()

    fun list(ids: List<DocumentId>): List<Document> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        return database
            .from(Documents)
            .leftJoin(UserTable, on = Documents.userId eq UserTable.id)
            .select()
            .where { Documents.id.inList(ids.map { it.value }) }
            .map(Documents::createEntity)
            .toList()
            .let { loadDocumentsWithHistory(it) }
    }

    fun list(sector: Sector): List<Document> =
        database
            .from(Documents)
            .leftJoin(UserTable, on = Documents.userId eq UserTable.id)
            .select()
            .where { Documents.sector eq sector.name }
            .map(Documents::createEntity)
            .toList()
            .let { loadDocumentsWithHistory(it) }

    fun findById(id: DocumentId): Document? =
        database
            .from(Documents)
            .leftJoin(UserTable, on = Documents.userId eq UserTable.id)
            .select()
            .where { (Documents.id eq id.value) }
            .map(Documents::createEntity)
            .firstOrNull()
            ?.let { loadDocumentWithHistory(it) }

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
            set(it.userId, record.createdBy.value)
            set(it.createdAt, record.createdAt.epochSecond)
        } as Number

        return findById(DocumentId(id.toLong()))!!
    }

    private fun loadDocumentsWithHistory(documents: List<Document>): List<Document> {
        if (documents.isEmpty()) return documents

        val documentIds = documents.mapNotNull { it.id }
        val historyMap = documentHistoryRepository.listByDocumentIds(documentIds)
            .groupBy { it.documentId }

        return documents.map { document ->
            val history = historyMap[document.id] ?: emptyList()
            document.copy(history = history)
        }
    }

    private fun loadDocumentWithHistory(document: Document): Document {
        val history = document.id?.let { documentHistoryRepository.listByDocumentId(it) } ?: emptyList()
        return document.copy(history = history)
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
    val createdByUsername: String?,
    val history: List<DocumentHistory> = emptyList(),
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
    val modifiedAt = long("modified_at")
    val deletedAt = long("deleted_at")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = Document(
        id = DocumentId(row.getOrFail(id)),
        name = row.getOrFail(name),
        number = row.getOrFail(number),
        observations = row[observations],
        sector = Sector(row.getOrFail(sector), null),
        type = row.getOrFail(type),
        createdBy = UserId(row.getOrFail(userId)),
        createdByUsername = row.getOrFail(UserTable.login),
        createdAt = row.getDateOrFail(createdAt),
        modifiedAt = row.getDate(modifiedAt),
        deletedAt = row.getDate(deletedAt),
    )
}
