package tender.hack.domain.entity

import java.math.BigDecimal
import java.util.UUID

data class CalculationResultEntity(
    val id: UUID,
    val sessionId: UUID,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val coeffVariation: Double?,
    val isHomogeneous: Boolean,
    val quantity: BigDecimal?,
    val method: String?,
)
