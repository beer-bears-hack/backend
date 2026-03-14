package tender.hack.service

import org.springframework.stereotype.Service
import tender.hack.domain.dto.SessionDto
import tender.hack.domain.dto.UserDto
import tender.hack.repository.UserRepository
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun createUser(): UserDto {
        val userEntity = userRepository.createSession()
        return UserDto(
            uuid = userEntity.uuid,
            created = userEntity.created
        )
    }

    fun getSessionByUuid(uuid: UUID): SessionDto {
        val userEntity = userRepository.findById(uuid)

        return SessionDto(
            userDto = UserDto(
                uuid = userEntity.uuid,
                created = userEntity.created
            ),
            //TODO: add session items from cte table
            items = emptyList()
        )
    }
}