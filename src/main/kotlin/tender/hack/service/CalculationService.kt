package tender.hack.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tender.hack.controller.requests.CalculateItemRequest
import tender.hack.controller.response.CalculateItemResponse
import tender.hack.controller.response.PriceRange
import tender.hack.repository.ContractRepository

/**
 * Service for calculating NMCK (initial procurement price)
 *
 * POST /calculate/item
 * Calculate price based on selected contracts + CTE items
 */
@Service
class CalculationService(
    private val contractRepository: ContractRepository
) {
    private val log = LoggerFactory.getLogger(CalculationService::class.java)

    fun calculate(request: CalculateItemRequest): CalculateItemResponse {
        log.info("Calculate request: ${request.items.size} items, quantity=${request.quantity}, method=${request.method}")

        // Get all prices from selected items
        val allPrices = mutableListOf<Double>()

        for (item in request.items) {
            // Get contract by contractId and cteId
            val contractPrices = contractRepository.findByContractIdAndCteId(item.contractId, item.cteId)
            allPrices.addAll(contractPrices.map { it.price })
        }

        log.info("All prices: $allPrices")

        // If no prices found, return default values
        if (allPrices.isEmpty()) {
            return CalculateItemResponse(
                unitPrice = 0.0,
                totalPrice = 0.0,
                priceRange = PriceRange(0.0, 0.0),
                coeffVariation = 0.0,
                isHomogeneous = true
            )
        }

        // Calculate statistics
        val minPrice = allPrices.minOrNull() ?: 0.0
        val maxPrice = allPrices.maxOrNull() ?: 0.0
        val avgPrice = allPrices.average()

        // Calculate coefficient of variation (CV = stdDev / mean)
        val stdDev = calculateStandardDeviation(allPrices)
        val coeffVariation = if (avgPrice > 0) stdDev / avgPrice else 0.0

        // Homogeneous if CV < 0.33 (33%)
        val isHomogeneous = coeffVariation < 0.33

        // Use average as unit price
        val unitPrice = avgPrice
        val totalPrice = unitPrice * request.quantity

        return CalculateItemResponse(
            unitPrice = unitPrice,
            totalPrice = totalPrice,
            priceRange = PriceRange(minPrice, maxPrice),
            coeffVariation = coeffVariation,
            isHomogeneous = isHomogeneous
        )
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0

        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
