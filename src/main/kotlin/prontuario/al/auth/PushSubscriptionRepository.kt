package prontuario.al.auth

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.long
import org.ktorm.schema.text
import org.springframework.stereotype.Repository
import prontuario.al.database.IBaseModel
import prontuario.al.ktorm.getDate
import prontuario.al.ktorm.getDateOrFail
import prontuario.al.ktorm.getOrFail
import java.time.Instant

@Repository
class PushSubscriptionRepository(
    private val database: Database,
) {
    fun saveRecord(record: PushSubscription): PushSubscription {
        val id = database.insertAndGenerateKey(PushSubscriptions) {
            set(it.userId, record.userId.value)
            set(it.endpoint, record.endpoint)
            set(it.p256dh, record.p256dh)
            set(it.auth, record.auth)
            set(it.createdAt, record.createdAt.epochSecond)
        } as Number

        return findById(PushSubscriptionId(id.toLong()))!!
    }

    fun findById(id: PushSubscriptionId): PushSubscription? =
        database
            .from(PushSubscriptions)
            .select()
            .where { (PushSubscriptions.id eq id.value) and (PushSubscriptions.deletedAt.isNull()) }
            .map(PushSubscriptions::createEntity)
            .firstOrNull()

    fun findByUserId(userId: UserId): List<PushSubscription> =
        database
            .from(PushSubscriptions)
            .select()
            .where { (PushSubscriptions.userId eq userId.value) and (PushSubscriptions.deletedAt.isNull()) }
            .map(PushSubscriptions::createEntity)
            .toList()

    fun findByUserIdAndEndpoint(
        userId: UserId,
        endpoint: String,
    ): PushSubscription? =
        database
            .from(PushSubscriptions)
            .select()
            .where {
                (PushSubscriptions.userId eq userId.value) and
                    (PushSubscriptions.endpoint eq endpoint) and
                    (PushSubscriptions.deletedAt.isNull())
            }
            .map(PushSubscriptions::createEntity)
            .firstOrNull()

    fun deleteAllByUserId(userId: UserId) {
        database.update(PushSubscriptions) {
            set(it.deletedAt, Instant.now().epochSecond)
            where {
                it.userId eq userId.value
            }
        }
    }

    fun delete(subscription: PushSubscription) {
        database.update(PushSubscriptions) {
            set(it.deletedAt, Instant.now().epochSecond)
            where {
                it.id eq subscription.id!!.value
            }
        }
    }
}

@JvmInline
value class PushSubscriptionId(
    val value: Long,
)

data class PushSubscription(
    val id: PushSubscriptionId?,
    val userId: UserId,
    val endpoint: String,
    val p256dh: String,
    val auth: String,
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant? = null,
    val deletedAt: Instant? = null,
) : IBaseModel {
    override fun getId(): Any? = id
}

object PushSubscriptions : BaseTable<PushSubscription>("push_subscriptions") {
    val id = long("id").primaryKey()
    val userId = long("user_id")
    val endpoint = text("endpoint")
    val p256dh = text("p256dh")
    val auth = text("auth")
    val createdAt = long("created_at")
    val modifiedAt = long("modified_at")
    val deletedAt = long("deleted_at")

    override fun doCreateEntity(
        row: QueryRowSet,
        withReferences: Boolean,
    ) = PushSubscription(
        id = PushSubscriptionId(row.getOrFail(id)),
        userId = UserId(row.getOrFail(userId)),
        endpoint = row.getOrFail(endpoint),
        p256dh = row.getOrFail(p256dh),
        auth = row.getOrFail(auth),
        createdAt = row.getDateOrFail(createdAt),
        modifiedAt = row.getDate(modifiedAt),
        deletedAt = row.getDate(deletedAt),
    )
}
