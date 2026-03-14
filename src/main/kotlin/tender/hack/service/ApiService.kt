package tender.hack.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tender.hack.domain.dto.CteDto
import tender.hack.domain.entity.CteEntity
import tender.hack.repository.CteRepository

@Service
class ApiService(
    private val cteRepository: CteRepository
) {

    fun findCategories(): List<String> {
        return cteRepository.findAllCategories()
    }

    fun findManufacturers(): List<String> {
        return cteRepository.findAllManufacturers()
    }

    fun takeCteInfoById(cteId: String): CteDto {
        val result = cteRepository.takeCteInfoById(cteId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return CteDto(
            result.id,
            result.cteId,
            result.cteName,
            result.category,
            result.manufacturer,
            result.characteristics
        )
    }
}