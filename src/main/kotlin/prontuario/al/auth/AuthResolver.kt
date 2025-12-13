package prontuario.al.auth

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.bouncycastle.crypto.generators.SCrypt
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import prontuario.al.auth.Users.sector
import prontuario.al.exception.CustomErrorClassification
import prontuario.al.exception.GraphqlException
import prontuario.al.exception.GraphqlExceptionErrorCode
import prontuario.al.generated.types.*
import prontuario.al.logger.GraphqlLoggingFilter.Companion.logger
import java.util.*
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
    private val pushSubscriptionRepository: PushSubscriptionRepository,
) {
    @PreAuthorize("true")
    @DgsQuery
    fun listUsers(): List<PublicUserInfo> =
        userRepository.list().map {
            PublicUserInfo(
                id = it.id!!.value.toInt(),
                username = it.login,
                sector = it.sector,
            )
        }

    @PreAuthorize("hasRole('ADMIN:READ')")
    @DgsQuery
    fun listSectors(): List<prontuario.al.generated.types.Sector> =
        sectorRepository.list().map {
            prontuario.al.generated.types
                .Sector(it.name, it.code)
        }

    @PreAuthorize("hasRole('ADMIN:READ')")
    @DgsQuery
    fun listUsersDetailed(): List<prontuario.al.generated.types.User> =
        userRepository.list().map {
            prontuario.al.generated.types.User(
                id = it.id!!.value.toString(),
                username = it.login,
                sector = prontuario.al.generated.types.Sector(
                    name = it.sector,
                    code = sectorRepository.list().firstOrNull { sector -> sector.name == it.sector }?.code,
                ),
                roles = RoleBasedGrantedAuthority.getAuthRoles().map { m -> Role(m.getRole(), m.getLevel()) }.toList(),
                isAuthenticated = false,
            )
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
                roles = mapOf(found.role to LevelEnum.WRITE),
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
    fun createUser(
        @InputArgument username: String,
        @InputArgument sector: String,
        @InputArgument role: RoleEnum,
    ): CreateUserResult {
        val found = userRepository.findUser(username, sector)
        if (found != null) {
            logger.warn { "Attempted to create user $username for sector $sector, but already exists" }
            throw GraphqlException("Usuario já existe", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.ALREADY_EXISTS)
        }

        logger.info { "Creating user $username" }
        val foundForOtherSector = userRepository.findUser(username)
        if (foundForOtherSector != null) {
            logger.info { "User $username already exists for another sector, reusing password hash" }
        } else {
            logger.info { "User does not exist from before" }
        }

        val password = if (foundForOtherSector == null) "tmp" + Random.nextInt() else "A mesma senha que antes"
        val passwordHash = if (foundForOtherSector != null) foundForOtherSector.password else hashPassword(password)

        logger.info { "Password hash created.." }

        val user = userRepository.saveRecord(
            User(
                id = null,
                login = username,
                sector = sector,
                password = passwordHash,
                role = role,
                requirePasswordReset = foundForOtherSector == null,
            ),
        )

        logger.info { "User $username for sector $sector created successfully" }

        return CreateUserResult(user.id!!.value.toInt(), password)
    }

    @PreAuthorize("hasRole('ADMIN:WRITE')")
    @DgsMutation
    fun createSector(
        @InputArgument name: String,
        @InputArgument code: String?,
    ): Response {
        val found = sectorRepository.list().firstOrNull { it.name == name }
        if (found != null) {
            if (found.deletedAt != null) {
                logger.warn { "Sector $sector already exists but has been deactivated, reactivating" }
                sectorRepository.reActivate(found)
                return Response(true)
            }
            logger.warn { "Attempted to create sector $sector, but already exists" }
            throw GraphqlException("Setor já existe", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.ALREADY_EXISTS)
        }

        logger.info { "Sector $name created successfully" }
        sectorRepository.saveRecord(Sector(name, code))
        return Response(true)
    }

    @PreAuthorize("hasRole('ADMIN:WRITE')")
    @DgsMutation
    fun deactivateSector(
        @InputArgument name: String,
    ): Response {
        val found = sectorRepository.list().firstOrNull { it.name == name }
            ?: throw GraphqlException("Setor não encontrado", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.NOT_FOUND)

        sectorRepository.deactivate(found)
        logger.info { "Sector $name deactivated successfully" }
        return Response(true)
    }

    @PreAuthorize("hasRole('ADMIN:WRITE')")
    @DgsMutation
    fun deactivateUser(
        @InputArgument username: String,
    ): Response {
        val found = userRepository.findUser(username)
            ?: throw GraphqlException("Usuario não encontrado", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.NOT_FOUND)

        userRepository.deactivate(found)
        logger.info { "User $username deactivated successfully" }
        return Response(true)
    }

    @PreAuthorize("hasRole('ADMIN:WRITE')")
    @DgsMutation
    fun resetPassword(
        @InputArgument username: String,
    ): CreateUserResult {
        val found = userRepository.findUser(username)
            ?: throw GraphqlException("Usuario não encontrado", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.NOT_FOUND)

        val password = "tmp" + Random.nextInt()
        val passwordHash = hashPassword(password)
        userRepository.update(
            User(
                id = found.id,
                login = found.login,
                sector = found.sector,
                password = passwordHash,
                role = found.role,
                requirePasswordReset = true,
            ),
        )
        logger.info { "Changed password for $username" }
        return CreateUserResult(found.id!!.value.toInt(), password)
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun resetOwnPassword(
        @InputArgument oldPassword: String,
        @InputArgument newPassword: String,
    ): CreateUserResult {
        val found = userRepository.findUser(AuthUtil.getUsername())
            ?: throw GraphqlException("Usuario não encontrado", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.NOT_FOUND)

        if (!found.isValid(oldPassword)) {
            throw GraphqlException(
                "Senha atual incorreta", CustomErrorClassification.BAD_REQUEST, errorCode = GraphqlExceptionErrorCode.VALIDATION
            )
        }

        val passwordHash = hashPassword(newPassword)
        userRepository.update(
            User(
                id = found.id,
                login = found.login,
                sector = found.sector,
                password = passwordHash,
                role = found.role,
                requirePasswordReset = false,
            ),
        )
        logger.info { "Changed password for ${AuthUtil.getUsername()}" }
        return CreateUserResult(found.id!!.value.toInt(), "A sua senha foi alterada com sucesso")
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun savePushSubscription(
        @InputArgument subscription: PushSubscriptionInput,
    ): Response {
        val userId = AuthUtil.getUserId()

        // Check if subscription already exists for this user and endpoint
        val existing = pushSubscriptionRepository.findByUserIdAndEndpoint(userId, subscription.endpoint)
        if (existing != null) {
            logger.info { "Push subscription already exists for user $userId and endpoint ${subscription.endpoint}" }
            return Response(true)
        }

        val pushSubscription = PushSubscription(
            id = null,
            userId = userId,
            endpoint = subscription.endpoint,
            p256dh = subscription.keys.p256dh,
            auth = subscription.keys.auth,
        )

        pushSubscriptionRepository.saveRecord(pushSubscription)
        logger.info { "Saved push subscription for user $userId" }
        return Response(true)
    }


    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun invalidatePushSubscription(
        @InputArgument subscription: PushSubscriptionInput,
    ): Response {
        val userId = AuthUtil.getUserId()
        pushSubscriptionRepository.findByUserIdAndEndpoint(AuthUtil.getUserId(), subscription.endpoint)?.let {
            pushSubscriptionRepository.delete(it)
            logger.info { "Invalidated push subscription for user $userId and endpoint ${subscription.endpoint}" }
        } ?: logger.info { "No push subscription found for user $userId and endpoint ${subscription.endpoint}" }
        return Response(true)
    }

    @PreAuthorize("hasRole('USER:WRITE')")
    @DgsMutation
    fun invalidateAllPushSubscriptions(): Response {
        val userId = AuthUtil.getUserId()
        pushSubscriptionRepository.deleteAllByUserId(userId)
        logger.info { "Invalidated all push subscriptions for user $userId" }
        return Response(true)
    }

    private fun hashPassword(password: String): String {
        val N = 16
        val r = 8
        val p = 1
        val salt = Random.nextBytes(16)
        val hash = SCrypt.generate(password.toByteArray(), salt, N, r, p, 32)
        return "$N:$r:$p:${Base64.getEncoder().encodeToString(salt + hash)}"
    }
}

@Service
class RateLimitingService {
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
