package tender.hack.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tender.hack.controller.requests.CalculateItemRequest
import tender.hack.controller.requests.SaveCalculationRequest
import tender.hack.controller.response.CalculateItemResponse
import tender.hack.service.CalculationService
import java.util.UUID

/**
 * POST /calculate/item
 * Calculate NMCK (initial procurement price) for selected prices.
 *
 * Request:
 * - items: List of selected items with contractId + cteId
 * - quantity: quantity for calculation
 * - method: calculation method (comparable_market_prices | tariff | cost)
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Calculation", description = "Price calculation endpoints")
class CalculationController(
    private val calculationService: CalculationService
) {

    @PostMapping("/calculate/item")
    @Operation(summary = "Calculate NMCK price", description = "Calculate initial procurement price based on selected contracts")
    fun calculateItem(@RequestBody request: CalculateItemRequest): ResponseEntity<CalculateItemResponse> {
        val result = calculationService.calculate(request)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/calculate/save")
    @Operation(summary = "Save calculation result", description = "Save calculation result to database for document generation")
    fun saveCalculation(
        @RequestBody request: SaveCalculationRequest,
        @RequestHeader("X-Session-Id") sessionId: String
    ): ResponseEntity<Unit> {
        val sessionUuid = UUID.fromString(sessionId)
        calculationService.saveCalculation(sessionUuid, request)
        return ResponseEntity.ok().build()
    }
}
