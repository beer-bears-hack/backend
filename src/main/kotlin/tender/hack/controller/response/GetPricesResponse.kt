package tender.hack.controller.response

import tender.hack.domain.dto.PriceDto

class GetPricesResponse(
    val steId: String,
    val prices: List<PriceResponse>,
) {
    data class PriceResponse(
        val id: Long,
        val price: Double,
        val date: String,
        val source: String,
        val isOutlier: Boolean,
        val reason: String?,
    )

    companion object {
        fun from(steId: String, prices: List<PriceDto>): GetPricesResponse {
            return GetPricesResponse(
                steId = steId,
                prices = prices.map { p ->
                    PriceResponse(
                        id = p.id,
                        price = p.price,
                        date = p.date.toString(),
                        source = p.source,
                        isOutlier = p.isOutlier,
                        reason = p.reason,
                    )
                }
            )
        }
    }
}
