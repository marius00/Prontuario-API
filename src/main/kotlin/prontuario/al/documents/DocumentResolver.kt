package prontuario.al.documents

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.springframework.security.access.prepost.PreAuthorize
import prontuario.al.generated.types.CreateDocumentResult
import prontuario.al.generated.types.DashboardDocuments
import prontuario.al.generated.types.DocumentInput


@DgsComponent
class DocumentResolver(
) {
    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun createDocument(
        @InputArgument input: DocumentInput,
    ): CreateDocumentResult {
        return CreateDocumentResult(true)
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
