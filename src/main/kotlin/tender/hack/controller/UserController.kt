package tender.hack.controller

import org.springframework.web.bind.annotation.*
import tender.hack.controller.response.CreateSessionResponse
import tender.hack.controller.response.GetSessionResponse
import tender.hack.controller.response.Session
import tender.hack.domain.dto.SessionDto
import tender.hack.service.UserService
import java.util.UUID

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
) {

    @PostMapping("/sessions")
    fun createSession(): CreateSessionResponse {
        val user = userService.createUser()
        return CreateSessionResponse(
            sessionId = user.uuid.toString(),
            createdAt = user.created
        )
    }

    @GetMapping("/sessions/{uuid}")
    fun getSession(@PathVariable uuid: String): GetSessionResponse {
        val session = userService.getSessionByUuid(UUID.fromString(uuid))

        return GetSessionResponse(
            id = session.userDto.uuid.toString(),
            createdAt = session.userDto.created,
            session = Session(
                session.items
            )
        )
    }

    @DeleteMapping("/sessions/{uuid}/items/{id}")
    fun deleteResultById(@PathVariable uuid: String, @PathVariable id: String) {
        userService.deleteResultById(
            UUID.fromString(uuid),
            UUID.fromString(id)
        )
    }
}