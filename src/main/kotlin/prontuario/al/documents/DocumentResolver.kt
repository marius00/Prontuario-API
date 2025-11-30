package prontuario.al.documents

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import prontuario.al.auth.AuthUtil
import prontuario.al.exception.GraphqlException
import prontuario.al.exception.GraphqlExceptionErrorCode
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
            createdBy = AuthUtil.getUserId(),
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
            id = doc.id.value.toInt(),
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
        val userSector = AuthUtil.getSector().name

        documents.forEach {
            if (documentRepository.findById(DocumentId(it.id.toLong()))?.sector?.name != userSector) {
                throw GraphqlException("Você não pode enviar um documento que não esteja em seu setor.", errorCode = GraphqlExceptionErrorCode.VALIDATION)
            }
        }

        documents.forEach {
            documentMovementRepository.saveRecord(
                prontuario.al.documents.DocumentMovement(
                    documentId = it.id.toLong(),
                    userId = AuthUtil.getUserId(),
                    fromSector = AuthUtil.getSector().name,
                    toSector = sector,
                )
            )

            documentHistoryRepository.saveRecord(
                DocumentHistory(
                    id = null,
                    documentId = DocumentId(it.id.toLong()),
                    action = DocumentHistoryTypeEnum.SENT,
                    sector = AuthUtil.getSector().name,
                    description = "Enviado para o setor $sector pelo ${AuthUtil.getUsername()}",
                )
            )
        }

        return Response(true)
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun acceptDocument(
        @InputArgument id: Int
    ): Document {
        val movement = documentMovementRepository.listToReceive(AuthUtil.getSector().name)
            .firstOrNull { it -> it.documentId == id.toLong() }
            ?: throw GraphqlException("Esse documento não foi encaminhado para seu setor.")

        documentMovementRepository.delete(movement)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = DocumentId(id.toLong()),
                action = DocumentHistoryTypeEnum.RECEIVED,
                sector = AuthUtil.getSector().name,
                description = "Recebido pelo ${AuthUtil.getUsername()}",
            )
        )

        val doc = documentRepository.findById(DocumentId(id.toLong()))
            ?: throw GraphqlException("Documento não encontrado.")

        doc.sector = AuthUtil.getSector()
        documentRepository.update(doc)

        return toGraphqlType(doc)
    }

    private fun toGraphqlType(doc: prontuario.al.documents.Document): Document {
        return Document(
            id = doc.id!!.value.toInt(),
            number = doc.number.toInt(),
            name = doc.name,
            observations = doc.observations,
            type = doc.type,
            sector = doc.sector,
            history = emptyList(),
        )
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun rejectDocument(
        @InputArgument id: Int
    ): Document {
        val movement = documentMovementRepository.listToReceive(AuthUtil.getSector().name)
            .firstOrNull { it -> it.documentId == id.toLong() }
            ?: throw GraphqlException("Esse documento não foi encaminhado para seu setor.")

        documentMovementRepository.delete(movement)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = DocumentId(id.toLong()),
                action = DocumentHistoryTypeEnum.REJECTED,
                sector = AuthUtil.getSector().name,
                description = "Rejeitado pelo ${AuthUtil.getUsername()}",
            )
        )

        // TODO: !Notification that document was rejected!

        val doc = documentRepository.findById(DocumentId(id.toLong()))
            ?: throw GraphqlException("Documento não encontrado.")
        return toGraphqlType(doc)
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun editDocument(
        @InputArgument input: ExistingDocumentInput
    ): Document {


        val doc = documentRepository.findById(DocumentId(input.id.toLong()))
            ?: throw GraphqlException("Documento não encontrado.")

        // TODO: Created at check! CreatedBy or From first log entry?
        if (doc.sector != AuthUtil.getSector())
            {
                throw GraphqlException("Você não pode editar um documento que não esteja em seu setor.", errorCode = GraphqlExceptionErrorCode.VALIDATION)
            }



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
