package tender.hack.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tender.hack.controller.response.GetPricesResponse
import tender.hack.service.PriceService

@RestController
@RequestMapping("/api")
class PriceController(
    private val priceService: PriceService
) {

    @GetMapping("/ste/{steId}/prices")
    fun getPrices(
        @PathVariable steId: String,
        @RequestParam(required = false) region: String?,
        @RequestParam(required = false, defaultValue = "12") period: Int
    ): GetPricesResponse {
        val prices = priceService.getPricesForSte(steId, region, period)
        return GetPricesResponse.from(steId, prices)
    }
}
