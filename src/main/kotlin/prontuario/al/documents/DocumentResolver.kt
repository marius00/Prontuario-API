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
import prontuario.al.logger.GraphqlLoggingFilter.Companion.logger
import java.time.Instant

@DgsComponent
@Transactional
class DocumentResolver(
    private val documentRepository: DocumentRepository,
    private val documentHistoryRepository: DocumentHistoryRepository,
    private val documentMovementRepository: DocumentMovementRepository,
    private val documentRequestRepository: DocumentRequestRepository,
) {
    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun createDocument(
        @InputArgument input: NewDocumentInput,
    ): prontuario.al.generated.types.Document {

        if (documentRepository.exists(input.number.toString())) {
            throw GraphqlException("Já existe um protocolo com esse número.", errorCode = GraphqlExceptionErrorCode.VALIDATION)
        }


        val document = prontuario.al.documents.Document(
            id = null,
            number = input.number.toString(),
            name = input.name,
            observations = input.observations,
            type = input.type,
            sector = AuthUtil.getSector(),
            createdBy = AuthUtil.getUserId(),
            createdByUsername = AuthUtil.getUsername(),
        )

        val doc = documentRepository.saveRecord(document)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = doc.id!!,
                action = DocumentHistoryTypeEnum.CREATED,
                sector = AuthUtil.getSector().name,
                description = "Registrado pelo ${AuthUtil.getUsername()}",
                userId = AuthUtil.getUserId(),
                username = AuthUtil.getUsername(),
            ),
        )

        return toGraphqlType(doc)
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun sendDocument(
        @InputArgument documents: List<Int>,
        @InputArgument sector: String,
    ): Response {
        val userSector = AuthUtil.getSector().name

        if (userSector == sector) {
            throw GraphqlException("Você não pode enviar um documento para o mesmo setor.", errorCode = GraphqlExceptionErrorCode.VALIDATION)
        }

        documents.forEach {
            if (documentRepository.findById(DocumentId(it.toLong()))?.sector?.name != userSector) {
                throw GraphqlException("Você não pode enviar um documento que não esteja em seu setor.", errorCode = GraphqlExceptionErrorCode.VALIDATION)
            }
        }

        documents.forEach {
            documentMovementRepository.saveRecord(
                prontuario.al.documents.DocumentMovement(
                    documentId = DocumentId(it.toLong()),
                    userId = AuthUtil.getUserId(),
                    fromSector = AuthUtil.getSector().name,
                    toSector = sector,
                ),
            )

            documentHistoryRepository.saveRecord(
                DocumentHistory(
                    id = null,
                    documentId = DocumentId(it.toLong()),
                    action = DocumentHistoryTypeEnum.SENT,
                    sector = AuthUtil.getSector().name,
                    description = "Enviado para o setor $sector pelo ${AuthUtil.getUsername()}",
                    userId = AuthUtil.getUserId(),
                    username = AuthUtil.getUsername(),
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
                userId = AuthUtil.getUserId(),
                username = AuthUtil.getUsername(),
            ),
        )

        val doc = documentRepository.findById(DocumentId(id.toLong())) ?: throw GraphqlException("Documento não encontrado.")

        doc.sector = AuthUtil.getSector()
        documentRepository.update(doc)

        return toGraphqlType(doc)
    }


    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun acceptDocuments(
        @InputArgument ids: List<Int>,
    ): List<prontuario.al.generated.types.Document> {
        return ids.map { it ->acceptDocument(it) }
    }

    private fun toGraphqlType(doc: prontuario.al.documents.Document): prontuario.al.generated.types.Document =
        Document(
            id = doc.id!!.value.toInt(),
            number = doc.number.toInt(),
            name = doc.name,
            observations = doc.observations,
            type = doc.type,
            sector = doc.sector,
            history = doc.history.map { history ->
                prontuario.al.generated.types.DocumentHistory(
                    action = history.action.toGraphqlType(),
                    user = history.username ?: "",
                    sector = prontuario.al.generated.types.Sector(history.sector, null),
                    dateTime = history.createdAt.toString(),
                    description = history.description ?: ""
                )
            },
            createdBy = doc.createdByUsername ?: "",
            createdAt = doc.createdAt.toString(),
            modifiedAt = (doc.modifiedAt ?: doc.createdAt).toString()
        )

    private fun DocumentHistoryTypeEnum.toGraphqlType(): DocumentActionEnum = when (this) {
        DocumentHistoryTypeEnum.CREATED -> DocumentActionEnum.CREATED
        DocumentHistoryTypeEnum.SENT -> DocumentActionEnum.SENT
        DocumentHistoryTypeEnum.RECEIVED -> DocumentActionEnum.RECEIVED
        DocumentHistoryTypeEnum.REJECTED -> DocumentActionEnum.REJECTED
        DocumentHistoryTypeEnum.REQUESTED -> DocumentActionEnum.REQUESTED
        DocumentHistoryTypeEnum.UPDATED -> DocumentActionEnum.MODIFIED
        DocumentHistoryTypeEnum.DELETED -> DocumentActionEnum.DELETED
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun rejectDocument(
        @InputArgument id: Int,
        @InputArgument description: String?,
    ): prontuario.al.generated.types.Document {
        val movement = documentMovementRepository.listForTargetSector(AuthUtil.getSector()).firstOrNull { it -> it.documentId!!.value == id.toLong() } ?: throw GraphqlException("Esse documento não foi encaminhado para seu setor.")

        documentMovementRepository.delete(movement)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = DocumentId(id.toLong()),
                action = DocumentHistoryTypeEnum.REJECTED,
                sector = AuthUtil.getSector().name,
                description = "Rejeitado pelo ${AuthUtil.getUsername()}" + if (description != null) "\n$description" else "",
                userId = AuthUtil.getUserId(),
                username = AuthUtil.getUsername(),
            ),
        )

        // TODO: !Notification that document was rejected!

        val doc = documentRepository.findById(DocumentId(id.toLong())) ?: throw GraphqlException("Documento não encontrado.")
        return toGraphqlType(doc)
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun cancelDocument(
        @InputArgument id: Int,
        @InputArgument description: String?,
    ): prontuario.al.generated.types.Document {
        val movement = documentMovementRepository.listForSourceSector(AuthUtil.getSector()).firstOrNull { it -> it.documentId!!.value == id.toLong() } ?: throw GraphqlException("Esse documento não foi encaminhado para seu setor.")

        documentMovementRepository.delete(movement)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = DocumentId(id.toLong()),
                action = DocumentHistoryTypeEnum.REJECTED,
                sector = AuthUtil.getSector().name,
                description = "Encaminhamento cancelado pelo ${AuthUtil.getUsername()}" + if (description != null) "\n$description" else "",
                userId = AuthUtil.getUserId(),
                username = AuthUtil.getUsername(),
            ),
        )

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
                doc.createdByUsername,
                doc.history,
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
                userId = AuthUtil.getUserId(),
                username = AuthUtil.getUsername(),
            ),
        )

        return toGraphqlType(ret)
    }

    @PreAuthorize("hasRole('USER:READ')")
    @DgsQuery
    fun listDocumentsForDashboard(): DashboardDocuments {
        val currentUserId = AuthUtil.getUserId()
        val currentSector = AuthUtil.getSector().name

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

        // Get requests made by current user
        val yourRequests = documentRequestRepository.findByUserIdWithDocuments(currentUserId)
            .map { requestWithDoc ->
                prontuario.al.generated.types.DocumentRequest(
                    type = DocumentRequestType.YOU_REQUESTED,
                    document = toGraphqlType(requestWithDoc.document),
                    reason = requestWithDoc.request.reason ?: "",
                    requestedBy = requestWithDoc.requestedByUsername,
                    requestedAt = requestWithDoc.request.createdAt.toString(),
                    sector = requestWithDoc.document.sector.name
                )
            }

        // Get requests made to current user's sector
        val requestsToYou = documentRequestRepository.findRequestsFromSectorWithDocuments(currentSector)
            .map { requestWithDoc ->
                prontuario.al.generated.types.DocumentRequest(
                    type = DocumentRequestType.SOMEONE_REQUESTED_FROM_YOU,
                    document = toGraphqlType(requestWithDoc.document),
                    reason = requestWithDoc.request.reason ?: "",
                    requestedBy = requestWithDoc.requestedByUsername,
                    requestedAt = requestWithDoc.request.createdAt.toString(),
                    sector = requestWithDoc.request.toSector
                )
            }

        val allRequests = yourRequests + requestsToYou

        return DashboardDocuments(
            inventory = inventory,
            inbox = inbox,
            outbox = outbox,
            requests = allRequests,
        )
    }

    @PreAuthorize("hasRole('USER:READ')")
    @DgsQuery
    fun listAllDocuments(
        @InputArgument since: String?,
    ): List<Document> {
        val sinceInstant = since?.let { Instant.parse(it) }
        return documentRepository.list(sinceInstant).map { it -> toGraphqlType(it) }
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun requestDocument(
        @InputArgument id: Int,
        @InputArgument reason: String,
    ): Response {
        val userId = AuthUtil.getUserId()
        val userSector = AuthUtil.getSector().name
        logger.info { "User $userId from sector $userSector is requesting document $id for reason: $reason" }

        val document = documentRepository.findById(DocumentId(id.toLong()))
            ?: throw GraphqlException("Document with id $id not found", errorCode = GraphqlExceptionErrorCode.NOT_FOUND)

        val documentSector = document.sector.name

        if (userSector == documentSector) {
            throw GraphqlException("Cannot request a document from your own sector", errorCode = GraphqlExceptionErrorCode.VALIDATION)
        }

        val documentRequest = DocumentRequest(
            id = null,
            documentId = DocumentId(id.toLong()),
            toSector = userSector,
            userId = userId,
            reason = reason,
        )

        documentRequestRepository.saveRecord(documentRequest)
        documentHistoryRepository.saveRecord(
            DocumentHistory(
                id = null,
                documentId = DocumentId(id.toLong()),
                action = DocumentHistoryTypeEnum.REQUESTED,
                sector = userSector,
                description = "Solicitado pelo ${AuthUtil.getUsername()} para o setor $documentSector. Motivo: $reason",
                userId = userId,
                username = AuthUtil.getUsername(),
            ),
        )

        return Response(true)
    }


    @PreAuthorize("hasRole('ADMIN:WRITE')")
    @DgsMutation
    fun deleteDocument(
        @InputArgument id: Int,
    ): Response {
        val userId = AuthUtil.getUserId()
        val userSector = AuthUtil.getSector().name
        logger.info { "User $userId from sector $userSector is deleting document $id" }

        val document = documentRepository.findById(DocumentId(id.toLong()))
            ?: throw GraphqlException("Document with id $id not found", errorCode = GraphqlExceptionErrorCode.NOT_FOUND)

        documentRepository.delete(document)

        return Response(true)
    }

}
