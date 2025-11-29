package prontuario.al.auth

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import de.mkammerer.argon2.Argon2Factory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import prontuario.al.auth.Users.sector
import prontuario.al.exception.CustomErrorClassification
import prontuario.al.exception.GraphqlException
import prontuario.al.exception.GraphqlExceptionErrorCode
import prontuario.al.generated.types.CreateUserResult
import prontuario.al.generated.types.LoginResult
import prontuario.al.generated.types.PublicUserInfo
import prontuario.al.generated.types.Response
import prontuario.al.logger.GraphqlLoggingFilter.Companion.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random


@DgsComponent
class AuthResolver(
    private val userRepository: UserRepository,
    private val tokenService: TokenService,
    private val rateLimiter: RateLimitingService,
    private val sectorRepository: SectorRepository,
) {
    @PreAuthorize("true")
    @DgsQuery
    fun listUsers(): List<PublicUserInfo> {
        return userRepository.list().map { PublicUserInfo(it.login, it.sector) }
    }

    @PreAuthorize("hasRole('ADMIN:READ')")
    @DgsQuery
    fun listSectors(): List<prontuario.al.generated.types.Sector> {
        return sectorRepository.list().map { prontuario.al.generated.types.Sector(it.name, it.code) }
    }


    @PreAuthorize("true")
    @DgsMutation
    fun login(
        @InputArgument sector: String,
        @InputArgument username: String,
        @InputArgument password: String,
    ): LoginResult {
        if (!rateLimiter.allowRequest(username)) {
            logger.warn { "Login attempt throttled for $username" }
            return LoginResult(null, false)
        }

        val found = userRepository.findUser(username, sector) ?: return LoginResult(null, false)
        if (found.isValid(password)) {
            val sectorCode = sectorRepository.list().firstOrNull { it.name == found.sector }?.code ?: ""
            val token = tokenService.generate(
                userId = found.id!!.value,
                username = found.login,
                roles = emptyMap(),
                sector = found.sector,
                sectorCode = sectorCode,
            )

            logger.info { "Login succeeded for $username" }
            return LoginResult(token, true)
        } else {
            logger.info { "Login failed for $username" }
            return LoginResult(null, false)
        }
    }


    @PreAuthorize("hasRole('ADMIN:WRITE')")
    @DgsMutation
    fun createUser(@InputArgument username: String, @InputArgument sector: String): CreateUserResult {
        val found = userRepository.findUser(username, sector)
        if (found != null) {
            logger.warn { "Attempted to create user $username for sector $sector, but already exists" }
            throw GraphqlException("Usuario já existe", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.ALREADY_EXISTS)
        }

        val password = "tmp" + Random.nextInt()

        userRepository.saveRecord(
            User(
                id = null,
                login = username,
                sector = sector,
                password = Argon2Factory.create().hash(2, 65536, 1, password),
                requirePasswordReset = true,
            )
        )

        logger.info { "User $username for sector $sector created successfully" }

        return CreateUserResult(password)
    }


    @PreAuthorize("hasRole('ADMIN:WRITE')")
    @DgsMutation
    fun createSector(@InputArgument name: String, @InputArgument code: String?): Response {
        val found = sectorRepository.list().any { it.name == name }
        if (found) {
            logger.warn { "Attempted to create sector $sector, but already exists" }
            throw GraphqlException("Setor já existe", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.ALREADY_EXISTS)
        }

        logger.info { "Sector $name created successfully" }
        sectorRepository.saveRecord(Sector(name, code))
        return Response(true)
    }
}

@Service
class RateLimitingService() {
    private val limit = 8
    private val timeWindowMillis = 30 * 1000
    private val requestCounts: ConcurrentMap<String?, AtomicInteger> = ConcurrentHashMap<String?, AtomicInteger>()
    private val lastRequestTimes: ConcurrentMap<String?, Long?> = ConcurrentHashMap<String?, Long?>()

    fun allowRequest(key: String?): Boolean {
        val currentTime = System.currentTimeMillis()
        lastRequestTimes.computeIfAbsent(key) { k: String? -> currentTime }
        requestCounts.computeIfAbsent(key) { k: String? -> AtomicInteger(0) }

        val lastTime: Long = lastRequestTimes[key]!!
        val count: AtomicInteger = requestCounts[key]!!

        if (currentTime - lastTime > timeWindowMillis) {
            count.set(1)
            lastRequestTimes.put(key, currentTime)
            return true
        } else {
            return count.incrementAndGet() <= limit
        }
    }
}


