package prontuario.al.auth

import de.mkammerer.argon2.Argon2Factory
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.*
import org.springframework.stereotype.Repository
import prontuario.al.database.IBaseModel
import prontuario.al.ktorm.getDate
import prontuario.al.ktorm.getDateOrFail
import prontuario.al.ktorm.getOrFail
import java.time.Instant

@Repository
class UserRepository(
    private val database: Database,
) {
    fun list(): List<User> =
        database
            .from(Users)
            .select()
            .where { Users.deletedAt.isNull() }
            .map(Users::createEntity)
            .toList()

    fun findUser(
        username: String,
        sector: String,
    ): User? =
        database
            .from(Users)
            .select()
            .where { (Users.login eq username) and (Users.sector eq sector) and (Users.deletedAt.isNull()) }
            .map(Users::createEntity)
            .firstOrNull()

    fun findUser(username: String): User? =
        database
            .from(Users)
            .select()
            .where { (Users.login eq username) and (Users.deletedAt.isNull()) }
            .map(Users::createEntity)
            .firstOrNull()

    fun findById(id: UserId): User? =
        database
            .from(Users)
            .select()
            .where { (Users.id eq id.value) and (Users.deletedAt.isNull()) }
            .map(Users::createEntity)
            .firstOrNull()

    fun saveRecord(record: User): User {
        val id = database.insertAndGenerateKey(Users) {
            set(it.login, record.login)
            set(it.password, record.password)
            set(it.sector, record.sector)
            set(it.role, record.role)
            set(it.requirePasswordReset, record.requirePasswordReset)
            set(it.createdAt, record.createdAt.epochSecond)
        } as Number

        return findById(UserId(id.toLong()))!!
    }

    fun update(record: User) {
        database.update(Users) {
            set(it.login, record.login)
            set(it.password, record.password)
            set(it.sector, record.sector)
            set(it.role, record.role)
            set(it.requirePasswordReset, record.requirePasswordReset)
            set(it.modifiedAt, Instant.now().epochSecond)
            where {
                it.id eq record.id!!.value
            }
        }
    }

    fun deactivate(record: User) {
        database.update(Users) {
            set(it.deletedAt, Instant.now().epochSecond)
            where {
                it.login eq record.login
            }
        }
    }
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
    val role: RoleEnum,
    val requirePasswordReset: Boolean,
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant? = null,
    val deletedAt: Instant? = null,
) : IBaseModel {
    override fun getId(): Any? = id

    fun isValid(password: String): Boolean = Argon2Factory.create().verify(this.password, password)
}

object Users : BaseTable<User>("user") {
    val id = long("id").primaryKey()
    val login = varchar("login")
    val password = varchar("password")
    val sector = varchar("sector")
    val role = enum<RoleEnum>("role")
    val requirePasswordReset = boolean("require_pwd_reset")
    val createdAt = long("created_at")
    val modifiedAt = long("modified_at")
    val deletedAt = long("deleted_at")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = User(
        id = UserId(row.getOrFail(id)),
        login = row.getOrFail(login),
        password = row[password],
        role = row.getOrFail(role),
        sector = row.getOrFail(sector),
        requirePasswordReset = row[requirePasswordReset] ?: false,
        createdAt = row.getDateOrFail(createdAt),
        modifiedAt = row.getDate(modifiedAt),
        deletedAt = row.getDate(deletedAt),
    )
}
