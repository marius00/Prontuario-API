package prontuario.al.documents

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.springframework.security.access.prepost.PreAuthorize
import prontuario.al.DocumentHistorys.DocumentHistory
import prontuario.al.DocumentHistorys.DocumentHistoryRepository
import prontuario.al.DocumentHistorys.DocumentHistoryTypeEnum
import prontuario.al.auth.AuthUtil
import prontuario.al.exception.GraphqlException
import prontuario.al.generated.types.*
import prontuario.al.generated.types.Document


@DgsComponent
class DocumentResolver(
    private val documentRepository: DocumentRepository,
    private val documentHistoryRepository: DocumentHistoryRepository,
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
        throw GraphqlException("Not yet implemented")
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun editDocument(
        @InputArgument input: ExistingDocumentInput
    ): Document {
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
