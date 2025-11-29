package prontuario.al.exception

import io.mockk.every
import io.mockk.mockkObject
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.HttpGraphQlTester
import prontuario.al.auth.AuthUtil
import prontuario.al.test.annotations.HttpLayerTest
import prontuario.al.test.annotations.MockAuthentication
import kotlin.test.Test

@HttpLayerTest
class GraphQLExceptionHandlerTest {
    @Autowired
    private lateinit var graphQlTester: HttpGraphQlTester

    @Test
    @MockAuthentication(username = "my user", roles = ["USER:WRITE"])
    fun `Should handle CustomExceptions correctly`() {
        mockkObject(AuthUtil) {
            val exception = GraphqlException(
                message = "This is a test",
                errorType = CustomErrorClassification.BAD_REQUEST,
            )

            every { AuthUtil.getUserId() } throws exception

            @Language("GraphQL")
            val query = "{ whoAmI { id } }"

            graphQlTester
                .document(query)
                .execute()
                .errors()
                .expect {
                    it.message == "This is a test" && it.errorType.toString() == CustomErrorClassification.BAD_REQUEST.name
                }
        }
    }
}
