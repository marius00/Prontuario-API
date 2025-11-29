package prontuario.al.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Healthcheck {
    @GetMapping("/healthcheck")
    fun healthcheck(): Result = Result("OK")
}

data class Result(
    val status: String,
)
