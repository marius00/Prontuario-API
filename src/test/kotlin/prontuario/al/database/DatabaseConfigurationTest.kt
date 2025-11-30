package prontuario.al.database

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import prontuario.al.test.annotations.DatabaseContainerInitializer.Companion.IMAGE_NAME
import javax.sql.DataSource
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
class DatabaseConfigurationTest {
    @Autowired
    lateinit var dataSource: DataSource

    @Test
    @Transactional
    fun `Should work just fine`() {
        assertEquals(dataSource.connection.metaData.url, springMySQLContainer.jdbcUrl)

        assertDoesNotThrow {
            dataSource.connection.prepareStatement("CREATE TABLE test_table (id INT AUTO_INCREMENT PRIMARY KEY)").executeUpdate()
        }
    }

    companion object {
        @JvmStatic
        @Container
        val springMySQLContainer: MySQLContainer<*> = MySQLContainer(DockerImageName.parse(IMAGE_NAME))

        @JvmStatic
        @DynamicPropertySource
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.driverClassName", springMySQLContainer::getDriverClassName)
            registry.add("spring.datasource.url", springMySQLContainer::getJdbcUrl)
            registry.add("spring.datasource.username", springMySQLContainer::getUsername)
            registry.add("spring.datasource.password", springMySQLContainer::getPassword)
        }
    }
}
