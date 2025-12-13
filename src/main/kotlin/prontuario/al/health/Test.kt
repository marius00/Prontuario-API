package prontuario.al.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import prontuario.al.notifications.NotificationService

@RestController
class TestController(private val notificationService: NotificationService) {
    @GetMapping("/test")
    fun healthcheck(): Result {
        //notificationService.sendNotification("Hello!", )
        return Result("OK")
    }
}
