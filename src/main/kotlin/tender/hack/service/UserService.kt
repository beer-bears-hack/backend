package tender.hack.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tender.hack.domain.dto.SessionDto
import tender.hack.domain.dto.SessionItemDto
import tender.hack.domain.dto.UserDto
import tender.hack.repository.CalculationResultRepository
import tender.hack.repository.UserRepository
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val calculationResultRepository: CalculationResultRepository,
    private val apiService: ApiService
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
        val calculationResult = calculationResultRepository.findResultsBySessionId(uuid)

        return SessionDto(
            userDto = UserDto(
                uuid = userEntity.uuid,
                created = userEntity.created
            ),
            items = calculationResult.map {
                val cteDto = apiService.takeCteInfoById(it.cteId)
                SessionItemDto(
                    name = cteDto.cteName,
                    category = cteDto.category,
                    quantity = it.quantity?.toInt() ?: 0,
                    unitPrice = it.unitPrice.toDouble(),
                    totalPrice = it.totalPrice.toDouble(),
                )
            }
        )
    }
}