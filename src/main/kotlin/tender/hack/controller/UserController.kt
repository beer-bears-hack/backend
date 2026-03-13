package tender.hack.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tender.hack.controller.response.CreateSessionResponse
import tender.hack.service.UserService

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
) {

    @PostMapping("/sessions")
    fun createSession(): CreateSessionResponse {
        val user = userService.createUser()
        return CreateSessionResponse(
            sessionId = user.uuid,
            createdAt = user.created
        )
    }

}