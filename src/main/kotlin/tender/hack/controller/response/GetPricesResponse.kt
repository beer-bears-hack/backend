package tender.hack.controller.response

import tender.hack.domain.dto.CteDto
import tender.hack.domain.dto.PriceDto

class GetPricesResponse(
    val cteDto: CteDto,
    val results: List<SearchResultItem>,
)
