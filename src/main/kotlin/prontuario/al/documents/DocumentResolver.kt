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
import java.time.Instant

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
    ): prontuario.al.generated.types.Document {
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
            ),
        )

        return prontuario.al.generated.types.Document(
            id = doc.id.value.toInt(),
            number = doc.number.toInt(),
            name = doc.name,
            observations = doc.observations,
            type = doc.type,
            sector = doc.sector,
            history = emptyList(),
            // createdAt = doc.createdAt.toString(),
            // modifiedAt = doc.modifiedAt?.toString(),
            // deletedAt = doc.deletedAt?.toString(),
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
                    documentId = DocumentId(it.id.toLong()),
                    userId = AuthUtil.getUserId(),
                    fromSector = AuthUtil.getSector().name,
                    toSector = sector,
                ),
            )

            documentHistoryRepository.saveRecord(
                DocumentHistory(
                    id = null,
                    documentId = DocumentId(it.id.toLong()),
                    action = DocumentHistoryTypeEnum.SENT,
                    sector = AuthUtil.getSector().name,
                    description = "Enviado para o setor $sector pelo ${AuthUtil.getUsername()}",
                ),
            )
        }

        return Response(true)
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun acceptDocument(
        @InputArgument id: Int,
    ): prontuario.al.generated.types.Document {
        val movement = documentMovementRepository.listForTargetSector(AuthUtil.getSector()).firstOrNull { it -> it.documentId!!.value == id.toLong() } ?: throw GraphqlException("Esse documento não foi encaminhado para seu setor.")

        documentMovementRepository.delete(movement)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = DocumentId(id.toLong()),
                action = DocumentHistoryTypeEnum.RECEIVED,
                sector = AuthUtil.getSector().name,
                description = "Recebido pelo ${AuthUtil.getUsername()}",
            ),
        )

        val doc = documentRepository.findById(DocumentId(id.toLong())) ?: throw GraphqlException("Documento não encontrado.")

        doc.sector = AuthUtil.getSector()
        documentRepository.update(doc)

        return toGraphqlType(doc)
    }

    private fun toGraphqlType(doc: prontuario.al.documents.Document): prontuario.al.generated.types.Document =
        Document(
            id = doc.id!!.value.toInt(),
            number = doc.number.toInt(),
            name = doc.name,
            observations = doc.observations,
            type = doc.type,
            sector = doc.sector,
            history = emptyList(),
        )

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun rejectDocument(
        @InputArgument id: Int,
    ): prontuario.al.generated.types.Document {
        val movement = documentMovementRepository.listForTargetSector(AuthUtil.getSector()).firstOrNull { it -> it.documentId!!.value == id.toLong() } ?: throw GraphqlException("Esse documento não foi encaminhado para seu setor.")

        documentMovementRepository.delete(movement)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = DocumentId(id.toLong()),
                action = DocumentHistoryTypeEnum.REJECTED,
                sector = AuthUtil.getSector().name,
                description = "Rejeitado pelo ${AuthUtil.getUsername()}",
            ),
        )

        // TODO: !Notification that document was rejected!

        val doc = documentRepository.findById(DocumentId(id.toLong())) ?: throw GraphqlException("Documento não encontrado.")
        return toGraphqlType(doc)
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun editDocument(
        @InputArgument input: ExistingDocumentInput,
    ): prontuario.al.generated.types.Document {
        val doc = documentRepository.findById(DocumentId(input.id.toLong())) ?: throw GraphqlException("Documento não encontrado.")

        if (doc.createdBy != AuthUtil.getUserId()) {
            throw GraphqlException("Você não pode editar um documento que não criado por você.", errorCode = GraphqlExceptionErrorCode.VALIDATION)
        }

        val ret = documentRepository.update(
            Document(
                doc.id,
                input.number.toString(),
                input.name,
                input.observations,
                input.type,
                doc.sector,
                doc.createdBy,
                doc.createdAt,
                Instant.now(),
            ),
        )

        val changes = mutableListOf<String>()
        if (doc.number != ret.number) {
            changes.add("número de ${doc.number} para ${ret.number}")
        }
        if (doc.name != ret.name) {
            changes.add("nome de '${doc.name}' para '${ret.name}'")
        }
        if (doc.observations != ret.observations) {
            changes.add("observações de '${doc.observations}' para '${ret.observations}'")
        }
        if (doc.type != ret.type) {
            changes.add("tipo de ${doc.type} para ${ret.type}")
        }

        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = doc.id!!,
                action = DocumentHistoryTypeEnum.UPDATED,
                sector = AuthUtil.getSector().name,
                description = "Modificado pelo ${AuthUtil.getUsername()}: " + changes.joinToString { it },
            ),
        )

        return toGraphqlType(ret)
    }

    @PreAuthorize("hasRole('USER:READ')")
    @DgsQuery
    fun listDocumentsForDashboard(): DashboardDocuments {
        val inbox = documentRepository
            .list(
                documentMovementRepository.listForTargetSector(AuthUtil.getSector()).map { it -> it.documentId!! }.toList(),
            ).map { it -> toGraphqlType(it) }
            .toList()

        val outbox = documentRepository
            .list(
                documentMovementRepository.listForSourceSector(AuthUtil.getSector()).map { it -> it.documentId!! }.toList(),
            ).map { it -> toGraphqlType(it) }
            .toList()

        val inventory = documentRepository.list(AuthUtil.getSector()).map { it -> toGraphqlType(it) }
            .filterNot { it in outbox }

        return DashboardDocuments(
            inventory = inventory,
            inbox = inbox,
            outbox = outbox,
        )
    }
}
