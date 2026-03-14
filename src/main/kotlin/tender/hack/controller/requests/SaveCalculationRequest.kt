package tender.hack.controller.requests

/**
 * POST /calculate/save
 * Save calculation result to database.
 *
 * Request body:
 * - unit_price: calculated unit price
 * - total_price: calculated total price
 * - price_range: min and max prices
 * - coeff_variation: coefficient of variation
 * - is_homogeneous: whether prices are homogeneous
 *
 * Session ID is passed via X-Session-Id header
 */
data class SaveCalculationRequest(
    val unitPrice: Double,
    val totalPrice: Double,
    val priceRange: PriceRangeInput,
    val coeffVariation: Double,
    val isHomogeneous: Boolean,
    val quantity: Int
)

data class PriceRangeInput(
    val min: Double,
    val max: Double
)
