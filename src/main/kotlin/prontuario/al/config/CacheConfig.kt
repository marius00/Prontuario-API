package prontuario.al.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(
            listOf(
                jwtAccessTokenCacheConfig(),
            ),
        )
        return cacheManager
    }

    @Bean
    fun jwtAccessTokenCacheConfig(): CaffeineCache =
        CaffeineCache(
            "jwtAccessToken", // Name of the cache used in @Cacheable
            Caffeine
                .newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(),
        )
}
