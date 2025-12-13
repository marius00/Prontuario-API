package prontuario.al.notifications

import com.interaso.webpush.VapidKeys
import com.interaso.webpush.WebPushService
import org.springframework.stereotype.Service
import prontuario.al.auth.PushSubscriptionRepository
import prontuario.al.auth.UserId
import prontuario.al.logger.GraphqlLoggingFilter.Companion.logger

@Service
class NotificationService(
    private val subscriptionRepository: PushSubscriptionRepository,
) {
    fun sendNotification(message: String, userId: UserId, type: NotificationType) {
        logger.info { "Sending notification..." }

        val vapidKeys = VapidKeys.fromUncompressedBytes(
            "BHvTYyUPD0e_STvP_ZVIkK3-hnOIaM3Us2jPoeMfrbE2ZI5klIDMVUJw7bGP1K13rvx9OoTSTXf0OQOJDdWCWMI",
            "Bx81vH7CbY3PEiRqHUvQuGd9KvVHqgIakabvKbxVO4c");

        val pushService = WebPushService(
            subject = "mailto:protocolo@evilsoft.net",
            vapidKeys = vapidKeys,
        )

        val subscription = subscriptionRepository.findByUserId(userId).firstOrNull() ?: return
        val subscriptionState = pushService.send(
            payload = message,
            endpoint = subscription.endpoint,
            p256dh = subscription.p256dh,
            auth = subscription.auth,
            urgency = com.interaso.webpush.WebPush.Urgency.Low,
            topic = type.name,
        )

        if (subscriptionState == com.interaso.webpush.WebPush.SubscriptionState.EXPIRED) {
            logger.info { "Subscription expired, deleting from database. Id=${subscription.id}" }
            subscriptionRepository.delete(subscription)
        } else {
            logger.info { "Notification sent successfully. Id=${subscription.id}" }
        }
    }
}
enum class NotificationType {
    REJECTION,
    REQUEST
}
