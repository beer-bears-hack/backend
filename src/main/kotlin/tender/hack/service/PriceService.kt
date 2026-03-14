package tender.hack.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tender.hack.controller.response.SearchResultItem
import tender.hack.domain.dto.PriceDto
import tender.hack.domain.entity.CteEntity
import tender.hack.repository.CteRepository
import java.time.LocalDate
import kotlin.random.Random

@Service
class PriceService(
    private val cteRepository: CteRepository,
    private val searchService: SearchService
) {

    /**
     * Search by chosen cte in ML model
     */
    fun getPricesForSte(steId: String, region: String?, period: Int): List<SearchResultItem> {
        val cteEntity = cteRepository.findByCteId(steId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        val response = searchService.search(
            cteEntity.cteName,
            cteEntity.category,
            cteEntity.manufacturer
        )

        return response
    }

}
