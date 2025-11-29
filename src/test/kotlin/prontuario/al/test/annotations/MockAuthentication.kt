package prontuario.al.test.annotations

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory
import prontuario.al.auth.AuthUser
import prontuario.al.auth.JwtAuthenticationFilter
import prontuario.al.auth.Role


@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory::class)
annotation class MockAuthentication(
    val username: String,
    val roles: Array<String>,
)

/**
 * Responsible for creating fake auth contexts for tests
 */
class WithMockCustomUserSecurityContextFactory : WithSecurityContextFactory<MockAuthentication> {
    override fun createSecurityContext(input: MockAuthentication): SecurityContext {
        val context = SecurityContextHolder.createEmptyContext()
        val principal = AuthUser(userName = input.username, userId = 123u)

        var adminLevel = ""

        // Create a key=>value map from the String[] with "KEY:VALUE" strings
        val roleMap = HashMap<String, String>()
        for (pairString in input.roles) {
            val parts = pairString.split(":")
            if (parts.size == 2) {
                val key = parts[0]
                val value = parts[1]
                roleMap[key] = value
                if (key == Role.ADMIN.name) {
                    adminLevel = value
                }
            }
        }

        if (adminLevel.isNotEmpty()) {
            for (role in Role.entries) {
                if (!roleMap.containsKey(role.name)) {
                    roleMap[role.name] = adminLevel
                }
            }
        }

        val auth: Authentication = UsernamePasswordAuthenticationToken(principal, "password", JwtAuthenticationFilter.createRoles(roleMap))
        context.authentication = auth
        return context
    }
}
