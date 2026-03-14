package tender.hack.controller.requests

class CalculateItemRequest(
    val quantity: Int,
    val selectedPriceIds: List<Long>,
    val manualPrices: List<ManualPrice>? = null,
    val method: String = "comparable_market_prices"
) {
    data class ManualPrice(
        val price: Double,
        val source: String
    )
}
