package prontuario.al.documents

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.long
import org.ktorm.schema.text
import org.ktorm.schema.varchar
import org.springframework.stereotype.Repository
import prontuario.al.auth.UserId
import prontuario.al.database.IBaseModel
import prontuario.al.ktorm.getDate
import prontuario.al.ktorm.getDateOrFail
import prontuario.al.ktorm.getOrFail
import java.time.Instant

@Repository
class DocumentRequestRepository(
    private val database: Database,
) {
    fun saveRecord(record: DocumentRequest): DocumentRequest {
        val id = database.insertAndGenerateKey(DocumentRequests) {
            set(it.documentId, record.documentId.value)
            set(it.toSector, record.toSector)
            set(it.userId, record.userId.value)
            set(it.reason, record.reason)
            set(it.createdAt, record.createdAt.epochSecond)
        } as Number

        return findById(DocumentRequestId(id.toLong()))!!
    }

    fun findById(id: DocumentRequestId): DocumentRequest? =
        database
            .from(DocumentRequests)
            .select()
            .where { (DocumentRequests.id eq id.value) and (DocumentRequests.deletedAt.isNull()) }
            .map(DocumentRequests::createEntity)
            .firstOrNull()

    fun findByDocumentId(documentId: DocumentId): List<DocumentRequest> =
        database
            .from(DocumentRequests)
            .select()
            .where { (DocumentRequests.documentId eq documentId.value) and (DocumentRequests.deletedAt.isNull()) }
            .map(DocumentRequests::createEntity)
            .toList()



    fun findByUserId(userId: UserId): List<DocumentRequest> =
        database
            .from(DocumentRequests)
            .select()
            .where { (DocumentRequests.userId eq userId.value) and (DocumentRequests.deletedAt.isNull()) }
            .map(DocumentRequests::createEntity)
            .toList()

    fun findRequestsFromSectorWithDocuments(sector: String): List<DocumentRequestWithDocument> =
        database
            .from(DocumentRequests)
            .innerJoin(Documents, on = DocumentRequests.documentId eq Documents.id)
            .leftJoin(prontuario.al.auth.Users, on = DocumentRequests.userId eq prontuario.al.auth.Users.id)
            .select()
            .where {
                (Documents.sector eq sector) and
                (DocumentRequests.deletedAt.isNull()) and
                (Documents.deletedAt.isNull())
            }
            .map { row ->
                DocumentRequestWithDocument(
                    request = DocumentRequests.createEntity(row),
                    document = Documents.createEntity(row),
                    requestedByUsername = row[prontuario.al.auth.Users.login] ?: ""
                )
            }
            .toList()

    fun findByUserIdWithDocuments(userId: UserId): List<DocumentRequestWithDocument> =
        database
            .from(DocumentRequests)
            .innerJoin(Documents, on = DocumentRequests.documentId eq Documents.id)
            .leftJoin(prontuario.al.auth.Users, on = DocumentRequests.userId eq prontuario.al.auth.Users.id)
            .select()
            .where {
                (DocumentRequests.userId eq userId.value) and
                (DocumentRequests.deletedAt.isNull()) and
                (Documents.deletedAt.isNull())
            }
            .map { row ->
                DocumentRequestWithDocument(
                    request = DocumentRequests.createEntity(row),
                    document = Documents.createEntity(row),
                    requestedByUsername = row[prontuario.al.auth.Users.login] ?: ""
                )
            }
            .toList()
}

@JvmInline
value class DocumentRequestId(
    val value: Long,
)

data class DocumentRequest(
    val id: DocumentRequestId?,
    val documentId: DocumentId,
    val toSector: String,
    val userId: UserId,
    val reason: String?,
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant? = null,
    val deletedAt: Instant? = null,
) : IBaseModel {
    override fun getId(): Any? = id
}

data class DocumentRequestWithDocument(
    val request: DocumentRequest,
    val document: Document,
    val requestedByUsername: String,
)

object DocumentRequests : BaseTable<DocumentRequest>("document_requests") {
    val id = long("id").primaryKey()
    val documentId = long("document_id")
    val toSector = varchar("to_sector")
    val userId = long("user_id")
    val reason = text("reason")
    val createdAt = long("created_at")
    val modifiedAt = long("modified_at")
    val deletedAt = long("deleted_at")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = DocumentRequest(
        id = DocumentRequestId(row.getOrFail(id)),
        documentId = DocumentId(row.getOrFail(documentId)),
        toSector = row.getOrFail(toSector),
        userId = UserId(row.getOrFail(userId)),
        reason = row[reason],
        createdAt = row.getDateOrFail(createdAt),
        modifiedAt = row.getDate(modifiedAt),
        deletedAt = row.getDate(deletedAt),
    )
}
