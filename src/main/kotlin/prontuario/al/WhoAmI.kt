package prontuario.al

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import org.springframework.security.core.context.SecurityContextHolder
import prontuario.al.auth.AuthUtil
import prontuario.al.auth.RoleBasedGrantedAuthority
import prontuario.al.auth.SectorRepository
import prontuario.al.auth.UserRepository
import prontuario.al.generated.types.Role
import prontuario.al.generated.types.Sector
import prontuario.al.generated.types.User

@DgsComponent
class WhoAmI(
    private val userRepository: UserRepository,
    private val sectorRepository: SectorRepository,
) {
    @DgsQuery
    fun whoAmI(): User? {
        if ("anonymousUser" == SecurityContextHolder
                .getContext()
                .authentication.principal
                .toString()
        ) {
            return User(null, null, null, emptyList(), false)
        }

        val fromDb = userRepository.findById(AuthUtil.getUserId())
        val sectorCode = sectorRepository.list().firstOrNull { it.name == fromDb?.sector }?.code

        return User(
            AuthUtil.getUserId().value.toString(),
            username = AuthUtil.getUsername(),
            sector = Sector(
                name = fromDb?.sector ?: "Error",
                code = sectorCode ?: "ERR",
            ),
            RoleBasedGrantedAuthority.getAuthRoles().map { Role(it.getRole(), it.getLevel()) }.toList(),
            SecurityContextHolder.getContext().authentication.isAuthenticated,
        )
    }
}
