package prontuario.al.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

data class AuthUser(
    val userId: Long,
    val userName: String,
) {
}

// Will need to see what our needs are, figure we'll have 2-3 different auth tokens.
// API tokens vs User web token
enum class Role {
    USER,
    ADMIN,
}

enum class Level {
    READ,
    WRITE,
    ;

    companion object {
        fun toList(value: Level): Array<Level> =
            when (value) {
                READ -> arrayOf(READ)
                WRITE -> arrayOf(READ, WRITE)
            }
    }
}

class RoleBasedGrantedAuthority(
    private val role: Role,
    private val level: Level,
) : GrantedAuthority {
    override fun getAuthority(): String = "ROLE_$role:$level"

    fun getRole(): Role = role

    fun getLevel(): Level = level

    companion object {
        fun getAuthRoles(): List<RoleBasedGrantedAuthority> {
            val authorities =
                SecurityContextHolder.getContext().authentication.authorities as Collection<GrantedAuthority>
            return authorities
                .stream()
                .map {
                    it as RoleBasedGrantedAuthority
                }.toList()
        }
    }
}
