package prontuario.al.auth

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import prontuario.al.generated.types.LoginResult
import prontuario.al.generated.types.PublicUserInfo
import prontuario.al.logger.GraphqlLoggingFilter.Companion.logger

@DgsComponent
class AuthResolver(private val userRepository: UserRepository, private val tokenService: TokenService) {
    @DgsQuery
    fun listUsers(): List<PublicUserInfo> {
        return userRepository.listUsers().map { PublicUserInfo(it.login, it.sector) }
    }
    @DgsMutation
    fun login(
        @InputArgument sector: String,
        @InputArgument username: String,
        @InputArgument password: String,
    ): LoginResult {
        val found = userRepository.findUser(username, sector) ?: return LoginResult(null, false)
        if (found.isValid(password)) {
            val token = tokenService.generate(
                userId = found.id!!.value,
                username = found.login,
                roles = emptyMap(),
            )

            logger.info { "Login succeeded for $username" }
            return LoginResult(token, true)
        } else {
            logger.info { "Login failed for $username" }
            return LoginResult(null, false)
        }
    }
}


