package prontuario.al

import prontuario.al.auth.AuthUtil
import prontuario.al.auth.Level
import prontuario.al.auth.RoleBasedGrantedAuthority
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import org.springframework.security.core.context.SecurityContextHolder

@DgsComponent
class WhoAmI {
    @DgsQuery
    fun whoAmI(): User? {
        if ("anonymousUser" == SecurityContextHolder
                .getContext()
                .authentication.principal
                .toString()
        ) {
            return User(null, emptyList(), false)
        }

        return User(
            AuthUtil.getUserId().toString(),
            RoleBasedGrantedAuthority.getAuthRoles().map { Role(it.getRole(), it.getLevel()) }.toList(),
            SecurityContextHolder.getContext().authentication.isAuthenticated,
        )
    }

    data class User(
        val id: String?,
        val roles: List<Role>,
        val isAuthenticated: Boolean,
    )

    data class Role(
        val role: prontuario.al.auth.Role,
        val level: Level,
    )
}
