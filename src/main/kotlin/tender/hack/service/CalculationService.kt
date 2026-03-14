package tender.hack.service

import org.springframework.stereotype.Service
import tender.hack.controller.requests.CalculateItemRequest
import tender.hack.controller.response.CalculateItemResponse
import kotlin.math.abs
import kotlin.math.sqrt

@Service
class CalculationService {

    /**
     * Calculate NMCK (initial procurement price) for selected prices.
     * Frontend can select/deselect prices to include in calculation.
     */
    fun calculateItem(request: CalculateItemRequest): CalculateItemResponse {
        // Mock prices - in real implementation, fetch from database by selectedPriceIds
        val selectedPrices = request.selectedPriceIds.map { id ->
            // Mock price value based on id
            (id * 100).toDouble()
        } + (request.manualPrices?.map { it.price } ?: emptyList())

        if (selectedPrices.isEmpty()) {
            throw IllegalArgumentException("No prices selected for calculation")
        }

        val minPrice = selectedPrices.minOrNull() ?: 0.0
        val maxPrice = selectedPrices.maxOrNull() ?: 0.0
        val avgPrice = selectedPrices.average()

        // Calculate coefficient of variation
        val stdDev = if (selectedPrices.size > 1) {
            val mean = avgPrice
            val variance = selectedPrices.map { (it - mean) * (it - mean) }.average()
            sqrt(variance)
        } else {
            0.0
        }

        val coeffVariation = if (avgPrice > 0) stdDev / avgPrice else 0.0

        // Check homogeneity (coefficient of variation < 0.33 = 33%)
        val isHomogeneous = coeffVariation < 0.33

        // Use average price as unit price for homogeneous data
        // For non-homogeneous, use median or exclude outliers
        val unitPrice = if (isHomogeneous) {
            avgPrice
        } else {
            // Exclude outliers and recalculate
            val filteredPrices = selectedPrices.filter { price ->
                abs(price - avgPrice) <= 2 * stdDev
            }
            if (filteredPrices.isNotEmpty()) filteredPrices.average() else avgPrice
        }

        val totalPrice = unitPrice * request.quantity

        return CalculateItemResponse(
            unitPrice = unitPrice,
            totalPrice = totalPrice,
            priceRange = CalculateItemResponse.PriceRange(minPrice, maxPrice),
            coeffVariation = coeffVariation,
            isHomogeneous = isHomogeneous
        )
    }
}
