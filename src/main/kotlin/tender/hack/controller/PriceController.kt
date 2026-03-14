package tender.hack.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tender.hack.controller.response.PriceResponse
import tender.hack.service.PriceService

/**
 * GET /ste/{steId}/prices
 * Get price data for a specific STE item.
 *
 * Query parameters:
 * - region: string (optional) - Filter by region
 * - period: number (optional, default: 12) - Period in months
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Prices", description = "Price analysis endpoints")
class PriceController(
    private val priceService: PriceService
) {

    @GetMapping("/ste/{steId}/prices")
    @Operation(summary = "Get prices for STE item", description = "Returns price history for a specific STE item")
    fun getPrices(
        @PathVariable steId: String,
        @RequestParam(required = false) region: String?,
        @RequestParam(required = false, defaultValue = "12") period: Int
    ): ResponseEntity<PriceResponse> {
        val prices = priceService.getPricesForSte(steId, region, period)
        return ResponseEntity.ok(PriceResponse(steId, prices))
    }
}
