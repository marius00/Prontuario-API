package prontuario.al.database

import org.ktorm.database.Database
import org.ktorm.database.SpringManagedTransactionManager
import org.ktorm.database.detectDialectImplementation
import org.ktorm.logging.detectLoggerImplementation
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class DatabaseConfiguration {
    @Bean
    fun dataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean
    fun dataSource(dataSourceProperties: DataSourceProperties): DataSource =
        dataSourceProperties
            .initializeDataSourceBuilder()
            .build()

    @Bean
    fun database(dataSource: DataSource): Database {
        val translator = SQLErrorCodeSQLExceptionTranslator(dataSource)

        return Database(
            transactionManager = SpringManagedTransactionManager(dataSource),
            dialect = detectDialectImplementation(),
            logger = detectLoggerImplementation(),
            exceptionTranslator = { ex -> translator.translate("Ktorm", null, ex) ?: ex },
        )
    }
}
