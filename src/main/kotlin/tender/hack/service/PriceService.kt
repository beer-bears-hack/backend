package tender.hack.service

import org.springframework.stereotype.Service
import tender.hack.domain.dto.PriceDto
import tender.hack.domain.entity.CteEntity
import tender.hack.repository.CteRepository
import java.time.LocalDate
import kotlin.random.Random

@Service
class PriceService(
    private val cteRepository: CteRepository
) {

    /**
     * Get prices for STE item.
     * Mock ML model: finds similar CTE items by category and returns mock prices.
     */
    fun getPricesForSte(steId: String, region: String?, period: Int): List<PriceDto> {
        // Mock ML: find similar CTE items by category
        val similarCtes = cteRepository.findSimilar(steId, limit = 10)

        if (similarCtes.isEmpty()) {
            return emptyList()
        }

        // Mock prices based on similar CTEs
        return similarCtes.mapIndexed { index, cte ->
            PriceDto(
                id = cte.id,
                price = generateMockPrice(cte),
                date = LocalDate.now().minusDays((index * 7).toLong()),
                source = "ML Similar CTE: ${cte.cteId}",
                isOutlier = false,
                reason = null,
            )
        }
    }

    private fun generateMockPrice(cte: CteEntity): Double {
        // Mock price generation based on CTE characteristics
        // In real implementation, this would come from ML model or contracts table
        val basePrice = when {
            cte.category?.contains("лекар", ignoreCase = true) == true -> Random.nextDouble(100.0, 5000.0)
            cte.category?.contains("техник", ignoreCase = true) == true -> Random.nextDouble(1000.0, 50000.0)
            cte.category?.contains("продук", ignoreCase = true) == true -> Random.nextDouble(50.0, 500.0)
            else -> Random.nextDouble(100.0, 10000.0)
        }
        return basePrice
    }
}
