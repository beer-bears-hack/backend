package tender.hack.controller.response

import tender.hack.service.CalculationService.PriceDataPoint

/**
 * Response for /calculate/item endpoint
 *
 * {
 *   "unit_price": 1200.0,
 *   "total_price": 1200.0,
 *   "price_range": { "min": 1000.0, "max": 1500.0 },
 *   "coeff_variation": 0.15,
 *   "is_homogeneous": true,
 *   "quantity": 1,
 *   "effective_sample_size": 15.5,
 *   "outliers_removed": 2,
 *   "no_data_reason": null
 * }
 */
data class CalculateItemResponse(
    val unitPrice: Double,
    val totalPrice: Double,
    val priceRange: PriceRange,
    val coeffVariation: Double,
    val isHomogeneous: Boolean,
    val quantity: Int,
    val effectiveSampleSize: Double = 0.0,
    val outliersRemoved: Int = 0,
    val noDataReason: String? = null,
    val cleanedData: List<PriceDataPoint>
)

data class PriceRange(
    val min: Double,
    val max: Double
)
