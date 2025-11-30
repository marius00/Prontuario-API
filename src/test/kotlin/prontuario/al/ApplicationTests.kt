package prontuario.al

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import prontuario.al.auth.LevelEnum.WRITE
import prontuario.al.auth.RoleEnum
import prontuario.al.auth.TokenService
import prontuario.al.test.annotations.DatabaseTest
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DatabaseTest
class ApplicationTests {
    @Autowired
    lateinit var tokenService: TokenService

    @Test
    fun `Should load app context`() {
    }

    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @Test
    fun `Should deny request without access token`() {
        val headers: HttpHeaders = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        // TODO!
        val body = """{"query":"query MyQuery {\n  someQueryHere() {\n    name \n  }\n}","operationName":"MyQuery"}"""
        val request = HttpEntity<String>(body, headers)
        val response = restTemplate!!.postForEntity<String?>("/graphql", request, String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode, response.body)
        assertEquals(
            """{"errors":[{"message":"org.springframework.security.authorization.AuthorizationDeniedException: Access Denied","locations":[{"line":2,"column":3}],"path":["listProductGroups"],"extensions":{"errorType":"PERMISSION_DENIED"}}],"data":null}""",
            response.body,
        )
    }

    @Test
    fun `Should load the graphiql endpoint without any headers`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_HTML

        val body = """{"query":"query MyQuery {\n  someQueryHere() {\n    name \n  }\n}","operationName":"MyQuery"}"""
        HttpEntity<String>(body, headers)
        val response = restTemplate!!.getForEntity<String?>("/graphiql", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode, response.body)
    }

    @Test
    fun `Should load the whoAmI graphql endpoint without any access tokens`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val body = """{"query":"query MyQuery {\n  whoAmI {\n    id\n    isAuthenticated\n    roles {\n      level\n      role\n    }\n  }\n}","operationName":"MyQuery"}"""

        val request = HttpEntity<String>(body, headers)
        val response = restTemplate!!.postForEntity<String?>("/graphql", request, String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode, response.body)
        assertEquals("""{"data":{"whoAmI":{"id":null,"isAuthenticated":false,"roles":[]}}}""", response.body)
    }

    @Test
    fun `Should load the whoAmI graphql endpoint with access tokens`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        headers.setBearerAuth(tokenService.generate(1234, "myUser", "s", "code", mapOf(RoleEnum.USER to WRITE)))
        val body = """{"query":"query MyQuery {\n  whoAmI {\n    id\n    isAuthenticated\n    roles {\n      level\n      role\n    }\n  }\n}","operationName":"MyQuery"}"""

        val request = HttpEntity<String>(body, headers)
        val response = restTemplate!!.postForEntity<String?>("/graphql", request, String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode, response.body)
        assertEquals(
            """{"data":{"whoAmI":{"id":"0","isAuthenticated":true,"roles":[{"level":"WRITE","role":"USER"}]}}}""",
            response.body,
        )
    }
}
