package prontuario.al.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import prontuario.al.generated.types.Sector

data class AuthUser(
    val userId: Long,
    val userName: String,
    val sector: Sector,
)

// Will need to see what our needs are, figure we'll have 2-3 different auth tokens.
// API tokens vs User web token
enum class RoleEnum {
    USER,
    ADMIN,
}

enum class LevelEnum {
    READ,
    WRITE,
    ;

    companion object {
        fun toList(value: LevelEnum): Array<LevelEnum> =
            when (value) {
                READ -> arrayOf(READ)
                WRITE -> arrayOf(READ, WRITE)
            }
    }
}

class RoleBasedGrantedAuthority(
    private val role: RoleEnum,
    private val level: LevelEnum,
) : GrantedAuthority {
    override fun getAuthority(): String = "ROLE_$role:$level"

    fun getRole(): RoleEnum = role

    fun getLevel(): LevelEnum = level

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
