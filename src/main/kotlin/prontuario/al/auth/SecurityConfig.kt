package prontuario.al.auth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig : WebMvcConfigurer {
    @Autowired
    private val environment: Environment? = null

    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = environment?.getProperty("cors.origins")?.split(",") ?: emptyList()

        registry
            .addMapping("/**")
            .allowedOrigins(*origins.toTypedArray())
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}

@RestController
class GraphQLOptionsController {
    @RequestMapping(method = [RequestMethod.OPTIONS], value = ["/graphql"])
    fun handleOptions(): ResponseEntity<Void> = ResponseEntity.ok().build()
}
