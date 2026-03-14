package tender.hack.controller.requests

/**
 * POST /calculate/item
 * Calculate NMCK (initial procurement price) for selected items.
 *
 * Request:
 * - items: List of selected items with contractId + cteId
 * - quantity: quantity for calculation
 * - method: calculation method
 */
data class CalculateItemRequest(
    val items: List<CalculateItem>,
    val quantity: Int = 1,
    val method: String = "comparable_market_prices"
)

data class CalculateItem(
    val contractId: String?,
    val cteId: String?,
    val price: Double?,
    val source: String?
)
