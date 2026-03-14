package tender.hack.controller.response

/**
 * Response for /calculate/item endpoint
 *
 * {
 *   "unit_price": 1200.0,
 *   "total_price": 1200.0,
 *   "price_range": { "min": 1000.0, "max": 1500.0 },
 *   "coeff_variation": 0.15,
 *   "is_homogeneous": true
 * }
 */
data class CalculateItemResponse(
    val unitPrice: Double,
    val totalPrice: Double,
    val priceRange: PriceRange,
    val coeffVariation: Double,
    val isHomogeneous: Boolean,
    val quantity: Int
)

data class PriceRange(
    val min: Double,
    val max: Double
)
