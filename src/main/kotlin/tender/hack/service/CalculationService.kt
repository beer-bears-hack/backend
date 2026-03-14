package tender.hack.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tender.hack.controller.requests.CalculateItemRequest
import tender.hack.controller.requests.SaveCalculationRequest
import tender.hack.controller.response.CalculateItemResponse
import tender.hack.controller.response.PriceRange
import tender.hack.domain.entity.CalculationResultEntity
import tender.hack.repository.CalculationResultRepository
import tender.hack.repository.ContractRepository
import java.math.BigDecimal
import java.util.UUID

/**
 * Service for calculating NMCK (initial procurement price)
 *
 * POST /calculate/item
 * Calculate price based on selected contracts + CTE items
 */
@Service
class CalculationService(
    private val contractRepository: ContractRepository,
    private val calculationResultRepository: CalculationResultRepository
) {
    private val log = LoggerFactory.getLogger(CalculationService::class.java)

    fun calculate(request: CalculateItemRequest): CalculateItemResponse {
        log.info("Calculate request: ${request.items.size} items, quantity=${request.quantity}, method=${request.method}")

        // Get all prices from selected items
        val allPrices = mutableListOf<Pair<Double, Boolean>>()

        for (item in request.items) {
            // Get contract by contractId and cteId
            if (item.contractId != null && item.cteId != null) {
                val contractPrices = contractRepository.findByContractIdAndCteId(item.contractId, item.cteId)
                allPrices.addAll(contractPrices.map { it.price to true})
            } else {
                //TODO: maybe add list of reasons in order to use it in final doc
                allPrices.add(
                    (item.price ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)) to false
                )
            }
        }

        log.info("All prices: $allPrices")

        // If no prices found, return default values
        if (allPrices.isEmpty()) {
            return CalculateItemResponse(
                unitPrice = 0.0,
                totalPrice = 0.0,
                priceRange = PriceRange(0.0, 0.0),
                coeffVariation = 0.0,
                isHomogeneous = true,
                quantity = request.quantity
            )
        }

        // Это лист только цен из БД
        val dataPricesList = allPrices.filter { it.second }.map { it.first }

        // Ищем окно и сколько данных достаточно


        // Calculate statistics
        val minPrice = dataPricesList.minOrNull() ?: 0.0
        val maxPrice = dataPricesList.maxOrNull() ?: 0.0
        val avgPrice = dataPricesList.average()

        // Calculate coefficient of variation (CV = stdDev / mean)
        val stdDev = calculateStandardDeviation(dataPricesList)
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
            isHomogeneous = isHomogeneous,
            quantity = request.quantity
        )
    }

    fun saveCalculation(sessionId: UUID, request: SaveCalculationRequest) {
        val entity = CalculationResultEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            unitPrice = BigDecimal.valueOf(request.unitPrice),
            totalPrice = BigDecimal.valueOf(request.totalPrice),
            minPrice = BigDecimal.valueOf(request.priceRange.min),
            maxPrice = BigDecimal.valueOf(request.priceRange.max),
            coeffVariation = request.coeffVariation,
            isHomogeneous = request.isHomogeneous,
            quantity = BigDecimal.valueOf(request.quantity.toLong()),
            method = null,
            cteId = request.cteId
        )
        calculationResultRepository.save(entity)
        log.info("Saved calculation result for session $sessionId")
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0

        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
