package tender.hack.controller.response

class CalculateItemResponse(
    val unitPrice: Double,
    val totalPrice: Double,
    val priceRange: PriceRange,
    val coeffVariation: Double,
    val isHomogeneous: Boolean
) {
    data class PriceRange(
        val min: Double,
        val max: Double
    )
}
