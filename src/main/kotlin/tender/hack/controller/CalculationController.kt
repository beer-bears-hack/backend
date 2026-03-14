package tender.hack.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tender.hack.controller.requests.CalculateItemRequest
import tender.hack.controller.response.CalculateItemResponse
import tender.hack.service.CalculationService

@RestController
@RequestMapping("/api")
class CalculationController(
    private val calculationService: CalculationService
) {

    @PostMapping("/calculate/item")
    fun calculateItem(@RequestBody request: CalculateItemRequest): CalculateItemResponse {
        return calculationService.calculateItem(request)
    }
}
