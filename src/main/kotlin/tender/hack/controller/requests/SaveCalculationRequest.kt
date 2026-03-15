package tender.hack.controller.requests

/**
 * POST /calculate/save
 * Save calculation result to database.
 *
 * Request body matches CalculateItemResponse format:
 * - unitPrice: calculated unit price
 * - totalPrice: calculated total price
 * - priceRange: min and max prices
 * - coeffVariation: coefficient of variation
 * - isHomogeneous: whether prices are homogeneous
 * - quantity: quantity for calculation
 * - effectiveSampleSize: KISH effective sample size (neff)
 * - outliersRemoved: number of outliers removed
 * - noDataReason: reason if calculation failed
 *
 * Session ID is passed via X-Session-Id header
 */
data class SaveCalculationRequest(
    val unitPrice: Double,
    val totalPrice: Double,
    val priceRange: PriceRangeInput,
    val coeffVariation: Double,
    val isHomogeneous: Boolean,
    val quantity: Int,
    val effectiveSampleSize: Double,
    val outliersRemoved: Int,
    val noDataReason: String?,
    val cteId: String
)

data class PriceRangeInput(
    val min: Double,
    val max: Double
)
