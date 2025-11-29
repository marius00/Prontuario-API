package prontuario.al.auth

import prontuario.al.auth.JwtAuthenticationFilter.Companion.createRoles
import prontuario.al.logger.GraphqlLoggingFilter.Companion.logger
import prontuario.al.logger.LoggingUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val myCachingService: TokenCachingService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader: String? = request.getHeader("Authorization")
        if (request.method == "OPTIONS") {
            filterChain.doFilter(request, response)
            return
        }

        try {
            if (authHeader.doesNotContainBearerToken()) {
                logger.warn("No bearer token found")
                filterChain.doFilter(request, response)
                return
            }

            // Isn't this always? Since we don't use sessions?
            val isNotLoggedIn =
                SecurityContextHolder.getContext().authentication == null ||
                        "anonymousUser" == SecurityContextHolder
                    .getContext()
                    .authentication.principal
                    .toString()
            if (isNotLoggedIn) {
                val jwtToken = authHeader!!.extractTokenValue()
                val token = myCachingService.parseToken(jwtToken)
                if (token != null) {
                    updateContext(token, request)
                }

                filterChain.doFilter(request, response)
            }
        } catch (ex: Exception) {
            logger.warn(ex.message, ex)
            filterChain.doFilter(request, response)
        }
    }

    companion object {
        inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? = runCatching { enumValueOf<T>(name) }.getOrNull()

        fun createRoles(roleMap: HashMap<String, String>): List<RoleBasedGrantedAuthority> {
            if (roleMap.containsKey(Role.ADMIN.name)) {
                for (role in Role.entries) {
                    roleMap[role.name] = roleMap[Role.ADMIN.name]!!
                }
            }

            val result = mutableListOf<RoleBasedGrantedAuthority>()
            for (kv in roleMap) {
                val role = enumValueOfOrNull<Role>(kv.key)
                if (role == null) {
                    logger.warn { "Role not found in roles: ${kv.key}" }
                    continue
                }

                // Cascading access level, eg WRITE gives you READ+WRITE
                for (level in Level.toList(Level.valueOf(kv.value))) {
                    result.add(RoleBasedGrantedAuthority(role, level))
                }
            }

            return result.toList()
        }
    }

    private fun String?.doesNotContainBearerToken() = this == null || !startsWith("Bearer ")

    private fun String.extractTokenValue() = substringAfter("Bearer ")

    private fun updateContext(
        token: UsernamePasswordAuthenticationToken,
        request: HttpServletRequest,
    ) {
        val u = token.principal as AuthUser?
        u?.let {
            LoggingUtil.put(u.userId, u.userName)
        }

        token.details = WebAuthenticationDetailsSource().buildDetails(request)

        SecurityContextHolder.getContext().authentication = token
    }
}

/**
 * This service exists solely to decouple the @Cacheable from the JwtAuthenticationFilter
 * This is because a OncePerRequestFilter cannot use @Cacheable or @Transactional (AOP)
 */
@Component
class TokenCachingService(
    private val tokenService: TokenService,
) {
    @CacheEvict(cacheNames = ["jwtAccessToken"], key = "#a0")
    fun invalidateCacheForToken(token: String) {
        logger.info { "Invalidating cache for token" }
    }

    @Cacheable("jwtAccessToken")
    fun parseToken(token: String): UsernamePasswordAuthenticationToken? {
        if (!tokenService.isValid(token)) {
            logger.warn { "Invalid token" }
            return null
        }
        val roles = createRoles(tokenService.getRoles(token))


        return UsernamePasswordAuthenticationToken(
            AuthUser(tokenService.getUserId(token), tokenService.getUsername(token)),
            null,
            roles,
        )
    }
}
