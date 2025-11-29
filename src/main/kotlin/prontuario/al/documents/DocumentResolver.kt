package prontuario.al.documents

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import prontuario.al.auth.AuthUtil
import prontuario.al.exception.GraphqlException
import prontuario.al.generated.types.*
import prontuario.al.generated.types.Document


@DgsComponent
@Transactional
class DocumentResolver(
    private val documentRepository: DocumentRepository,
    private val documentHistoryRepository: DocumentHistoryRepository,
    private val documentMovementRepository: DocumentMovementRepository,
) {
    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun createDocument(
        @InputArgument input: NewDocumentInput,
    ): Document {
        val document = prontuario.al.documents.Document(
            id = null,
            number = input.number.toString(),
            name = input.name,
            observations = input.observations,
            type = input.type,
            sector = AuthUtil.getSector(),
        )

        val doc = documentRepository.saveRecord(document)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = doc.id!!,
                action = DocumentHistoryTypeEnum.CREATED,
                sector = AuthUtil.getSector().name,
                description = "Registrado pelo ${AuthUtil.getUsername()}",
            )
        )

        return prontuario.al.generated.types.Document(
            id = doc.id.toInt(),
            number = doc.number.toInt(),
            name = doc.name,
            observations = doc.observations,
            type = doc.type,
            sector = doc.sector,
            history = emptyList(),
            //createdAt = doc.createdAt.toString(),
            //modifiedAt = doc.modifiedAt?.toString(),
            //deletedAt = doc.deletedAt?.toString(),
        )
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun sendDocument(
        @InputArgument documents: List<ExistingDocumentInput>,
        @InputArgument sector: String,
    ): Response {
        // documentMovementRepository
        // TODO: Create the DocumentMovement entry for user+id
        // TODO: Create DocumentHistory entry
        throw GraphqlException("Not yet implemented")
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun acceptDocument(
        @InputArgument id: Int
    ): Document {
        val movement = documentMovementRepository.list(AuthUtil.getUserId()) //TODO: Wrong! Its by sector!
            .firstOrNull { it -> it.documentId == id.toLong() }
            ?: throw GraphqlException("Esse documento não foi encaminhado para você.")

        documentMovementRepository.delete(movement)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = id.toLong(),
                action = DocumentHistoryTypeEnum.RECEIVED,
                sector = AuthUtil.getSector().name,
                description = "Recebido pelo ${AuthUtil.getUsername()}",
            )
        )

        // TODO: Update document location!

        throw GraphqlException("Not yet implemented") // TODO!
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun rejectDocument(
        @InputArgument id: Int
    ): Document {

        val movement = documentMovementRepository.list(AuthUtil.getUserId())//TODO: Wrong! Its by sector!
            .firstOrNull { it -> it.documentId == id.toLong() }
            ?: throw GraphqlException("Esse documento não foi encaminhado para você.")

        documentMovementRepository.delete(movement)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = id.toLong(),
                action = DocumentHistoryTypeEnum.REJECTED,
                sector = AuthUtil.getSector().name,
                description = "Rejeitado pelo ${AuthUtil.getUsername()}",
            )
        )

        // TODO: !Notification that document was rejected!

        throw GraphqlException("Not yet implemented")
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun editDocument(
        @InputArgument input: ExistingDocumentInput
    ): Document {
        // TODO: Find the DocumentMovement entry for user+id
        // TODO: Update the DocumentMovement entry
        // TODO: Create DocumentHistory entry
        throw GraphqlException("Not yet implemented")
    }


    @PreAuthorize("hasRole('USER:READ')")
    @DgsQuery
    fun listDocumentsForDashboard(): DashboardDocuments {
        return DashboardDocuments(
            inventory = emptyList(),
            inbox = emptyList(),
            outbox = emptyList(),
        )
    }
}
