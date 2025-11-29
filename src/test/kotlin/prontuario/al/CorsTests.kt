package prontuario.al

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import prontuario.al.auth.Level.WRITE
import prontuario.al.auth.Role
import prontuario.al.auth.TokenService
import prontuario.al.test.annotations.DatabaseTest
import kotlin.test.DefaultAsserter.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DatabaseTest
class CorsTests {
    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @Autowired
    lateinit var tokenService: TokenService

    @Test
    fun `should return CORS headers for preflight OPTIONS request with allowed origin`() {
        val headers = HttpHeaders().apply {
            set("Origin", "https://example.com")
            set("Access-Control-Request-Method", "POST")
            set("Access-Control-Request-Headers", "Content-Type")
        }

        headers.setBearerAuth(tokenService.generate(1234u, "myUser", mapOf(Role.USER to WRITE)))
        val entity = HttpEntity<String>(headers)

        val response: ResponseEntity<String> = restTemplate!!.exchange(
            "/graphql",
            HttpMethod.OPTIONS,
            entity,
            String::class.java,
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val responseHeaders = response.headers
        assertEquals(
            "https://example.com",
            responseHeaders.getFirst("Access-Control-Allow-Origin"),
            "Should allow the configured origin",
        )
        assertEquals(
            "true",
            responseHeaders.getFirst("Access-Control-Allow-Credentials"),
            "Should allow credentials",
        )
        assertEquals(
            "3600",
            responseHeaders.getFirst("Access-Control-Max-Age"),
            "Should set max age to 3600 seconds",
        )

        val allowedMethods = responseHeaders.getFirst("Access-Control-Allow-Methods")
        assertNotNull(allowedMethods, "Should have Access-Control-Allow-Methods header")
        assertEquals("GET,POST,OPTIONS", allowedMethods)
    }

    @Test
    fun `should return CORS headers for preflight OPTIONS request when origin is null`() {
        // Arrange
        val headers = HttpHeaders().apply {
            set("Access-Control-Request-Method", "POST")
            set("Access-Control-Request-Headers", "Content-Type")
        }
        headers.setBearerAuth(tokenService.generate(1234u, "myUser", mapOf(Role.USER to WRITE)))
        val entity = HttpEntity<String>(headers)

        // Act
        val response: ResponseEntity<String> = restTemplate!!.exchange(
            "/graphql",
            HttpMethod.OPTIONS,
            entity,
            String::class.java,
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val responseHeaders = response.headers
        assertNull(responseHeaders.getFirst("Access-Control-Allow-Origin"))
    }

    @Test
    fun `should return deny CORS-options request for unknown origin`() {
        // Arrange
        val headers = HttpHeaders().apply {
            set("Origin", "https://malicious-actor.example.com")
            set("Access-Control-Request-Method", "POST")
            set("Access-Control-Request-Headers", "Content-Type")
        }
        headers.setBearerAuth(tokenService.generate(1234u, "myUser", mapOf(Role.USER to WRITE)))
        val entity = HttpEntity<String>(headers)

        // Act
        val response: ResponseEntity<String> = restTemplate!!.exchange(
            "/graphql",
            HttpMethod.OPTIONS,
            entity,
            String::class.java,
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }
}
