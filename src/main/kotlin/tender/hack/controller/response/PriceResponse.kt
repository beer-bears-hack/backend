package tender.hack.controller.response

import tender.hack.domain.dto.PriceDto
import java.time.LocalDate

data class PriceResponse(
    val steId: String,
    val prices: List<PriceDto>
)