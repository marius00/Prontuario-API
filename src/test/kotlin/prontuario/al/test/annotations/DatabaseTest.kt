package prontuario.al.test.annotations

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@Target(AnnotationTarget.CLASS)
@SpringBootTest
@ContextConfiguration(initializers = [DatabaseContainerInitializer::class])
annotation class DatabaseTest

class DatabaseContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext?> {
    var mysqlContainer = MySQLContainer(DockerImageName.parse(IMAGE_NAME))

    override fun initialize(ctx: ConfigurableApplicationContext) {
        println("Starting MySQL test container")

        mysqlContainer.withReuse(true).start()

        TestPropertyValues
            .of(
                "spring.datasource.driverClassName=${mysqlContainer.driverClassName}",
                "spring.datasource.url=${mysqlContainer.jdbcUrl}",
                "spring.datasource.username=${mysqlContainer.username}",
                "spring.datasource.password=${mysqlContainer.password}",
                "spring.flyway.enabled=true",
                "spring.flyway.url=${mysqlContainer.jdbcUrl}",
                "spring.flyway.user=${mysqlContainer.username}",
                "spring.flyway.password=${mysqlContainer.password}",
            ).applyTo(ctx.environment)
    }

    companion object {
        const val IMAGE_NAME = "mysql:8.0.39"
    }
}
