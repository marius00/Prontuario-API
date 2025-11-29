package prontuario.al.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class TokenService {
    @Autowired
    private val environment: Environment? = null

    fun generate(
        userId: Long,
        username: String,
        roles: Map<Role, Level>,
    ): String {
        val secret = SecretKeySpec(environment?.getProperty("secrets.jwt")?.toByteArray(), "HmacSHA256")
        return Jwts
            .builder()
            .header()
            .type("JWT")
            .and()
            .claims()
            .issuer("Self")
            .subject(userId.toString())
            .id("12345")
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + 3600 * 24 * 60 * 60 * 1000))
            .add("userId", userId)
            .add("username", username)
            .add("roles", roles)
            .and()
            .signWith(secret)
            .compact()
    }

    fun isValid(token: String): Boolean = !isExpired(token)

    fun getSubject(token: String): String? = getAllClaims(token).subject

    fun isExpired(token: String): Boolean =
        getAllClaims(token)
            .expiration
            .before(Date(System.currentTimeMillis()))

    private fun getAllClaims(token: String): Claims {
        val secret = Keys.hmacShaKeyFor(environment?.getProperty("secrets.jwt")?.toByteArray())
        val parser = Jwts
            .parser()
            .verifyWith(secret)
            .build()

        return parser
            .parseSignedClaims(token)
            .payload
    }

    fun getRoles(token: String): HashMap<String, String> {
        val secret = Keys.hmacShaKeyFor(environment?.getProperty("secrets.jwt")?.toByteArray())
        val parser = Jwts
            .parser()
            .verifyWith(secret)
            .build()

        val roles = parser
            .parseSignedClaims(token)
            .payload["roles"]
            ?: throw Exception("Could not find any roles")

        @Suppress("UNCHECKED_CAST")
        return roles as HashMap<String, String>
    }

    fun getUsername(token: String): String = getAllClaims(token)["username"]?.toString() ?: throw Exception("Token is missing username")
    fun getUserId(token: String): UInt = getAllClaims(token)["userId"]?.toString()?.toUInt() ?: throw Exception("Token is missing userId")
}
