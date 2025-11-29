package prontuario.al

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import org.springframework.security.core.context.SecurityContextHolder
import prontuario.al.auth.AuthUtil
import prontuario.al.auth.Level
import prontuario.al.auth.RoleBasedGrantedAuthority

@DgsComponent
class WhoAmI {
    @DgsQuery
    fun whoAmI(): User? {
        if ("anonymousUser" == SecurityContextHolder
                .getContext()
                .authentication.principal
                .toString()
        ) {
            return User(null, emptyList(), null, null,false)
        }

        return User(
            AuthUtil.getUserId().toString(),
            RoleBasedGrantedAuthority.getAuthRoles().map { Role(it.getRole(), it.getLevel()) }.toList(),
            username = AuthUtil.getUsername(),
            sector = "TODO",
            SecurityContextHolder.getContext().authentication.isAuthenticated,
        )
    }

    data class User(
        val id: String?,
        val roles: List<Role>,
        val username: String?,
        val sector: String?,
        val isAuthenticated: Boolean,
    )

    data class Role(
        val role: prontuario.al.auth.Role,
        val level: Level,
    )
}
