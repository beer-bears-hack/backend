package tender.hack.service

import org.springframework.stereotype.Service
import tender.hack.domain.dto.UserDto
import tender.hack.repository.UserRepository

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
}