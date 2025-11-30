package prontuario.al.documents

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import org.springframework.stereotype.Repository
import prontuario.al.auth.UserId
import prontuario.al.database.IBaseModel
import prontuario.al.ktorm.getOrFail

@Repository
class DocumentMovementRepository(private val database: Database) {
    fun list(): List<DocumentMovement> {
        return database
            .from(DocumentMovements)
            .select()
            .map(DocumentMovements::createEntity)
            .toList()
    }

    fun listToReceive(sector: String): List<DocumentMovement> {
        return database
            .from(DocumentMovements)
            .select()
            .where { DocumentMovements.toSector.eq(sector) }
            .map(DocumentMovements::createEntity)
            .toList()
    }

    fun delete(movement: DocumentMovement) {
        database
            .delete(DocumentMovements) { DocumentMovements.documentId eq movement.documentId!! }
    }

    fun findById(id: Long): DocumentMovement? =
        database
            .from(DocumentMovements)
            .select()
            .where { (DocumentMovements.documentId eq id) }
            .map(DocumentMovements::createEntity)
            .firstOrNull()

    fun saveRecord(record: DocumentMovement): DocumentMovement {
        val id = database.insertAndGenerateKey(DocumentMovements) {
            set(it.documentId, record.documentId)
            set(it.fromSector, record.fromSector)
            set(it.toSector, record.toSector)
            set(it.userId, record.userId.value)
        } as Number

        return findById(id.toLong())!!
    }
}

data class DocumentMovement(
    val documentId: Long?,
    val userId: UserId,
    val fromSector: String,
    val toSector: String,
) : IBaseModel {
    override fun getId(): Any? = documentId
}

object DocumentMovements : BaseTable<DocumentMovement>("document_history") {
    val documentId = long("document_id").primaryKey()
    val userId = long("user_id")
    val fromSector = varchar("from_sector")
    val toSector = varchar("to_sector")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = DocumentMovement(
        documentId = row.getOrFail(documentId),
        userId = UserId(row.getOrFail(userId)),
        fromSector = row.getOrFail(fromSector),
        toSector = row.getOrFail(toSector),
    )
}
