package prontuario.al.auth

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import prontuario.al.generated.types.Sector
import javax.security.sasl.AuthenticationException

/**
 * Fetches the currently logged-in user
 */
class AuthUtil {
    companion object {
        fun getUserId(): Long {
            val authUser = SecurityContextHolder.getContext().authentication.principal as AuthUser?
            if (authUser != null) {
                return authUser.userId
            }

            throw AccessDeniedException("Invalid user principal")
        }

        fun getUsername(): String {
            val authUser = SecurityContextHolder.getContext().authentication.principal as AuthUser?
            if (authUser != null) {
                return authUser.userName
            }

            throw AccessDeniedException("Invalid user principal")
        }

        fun getSector(): Sector {
            val authUser = SecurityContextHolder.getContext().authentication.principal as AuthUser?
            if (authUser != null) {
                return authUser.sector
            }

            throw AccessDeniedException("Invalid user principal")
        }

        fun getUserIdNullable(): UInt? {
            val principal = SecurityContextHolder
                .getContext()
                .authentication.principal
                .toString()
            return principal.toUIntOrNull()
        }

        fun assert(userId: UInt) {
            val principal = SecurityContextHolder
                .getContext()
                .authentication.principal
                .toString()
            if (principal.toUIntOrNull() != userId) {
                throw AuthenticationException("User $userId not found for this tenant")
            }
        }
    }
}
