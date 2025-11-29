package prontuario.al.auth

import de.mkammerer.argon2.Argon2Factory
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.boolean
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import org.springframework.stereotype.Repository
import prontuario.al.database.IBaseModel
import prontuario.al.ktorm.getDate
import prontuario.al.ktorm.getDateOrFail
import prontuario.al.ktorm.getOrFail
import java.time.Instant

@Repository
class UserRepository(private val database: Database) {
    fun listUsers(): List<User> {
        return database
            .from(Users)
            .select()
            .map(Users::createEntity)
            .toList()
    }

    fun findUser(username: String, sector: String): User? =
        database
            .from(Users)
            .select()
            .where { (Users.login eq username) and (Users.sector eq sector) }
            .map(Users::createEntity)
            .firstOrNull()
}

@JvmInline
value class UserId(
    val value: Long,
)

data class User(
    val id: UserId?,
    val login: String,
    val password: String?,
    val sector: String,
    val requirePasswordReset: Boolean,
    val createdAt: Instant,
    val modifiedAt: Instant?,
    val deletedAt: Instant?,
) : IBaseModel {
    override fun getId(): Any? = id

    fun isValid(password: String): Boolean {
        return Argon2Factory.create().verify(this.password, password)
    }
}

object Users : BaseTable<User>("user") {
    val id = long("id").primaryKey()
    val login = varchar("login")
    val password = varchar("password")
    val sector = varchar("sector")
    val requirePasswordReset = boolean("require_pwd_reset")
    val createdAt = long("created_at")
    val modifiedAt = long("updated_at")
    val deletedAt = long("deleted_at")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = User(
        id = UserId(row.getOrFail(id)),
        login = row.getOrFail(login),
        password = row[password],
        sector = row.getOrFail(sector),
        requirePasswordReset = row[requirePasswordReset] ?: false,
        createdAt = row.getDateOrFail(createdAt),
        modifiedAt = row.getDate(modifiedAt),
        deletedAt = row.getDate(deletedAt),
    )
}
