package tender.hack.domain.entity

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CalculationItemEntity(
    val id: Long? = null,
    val calculationId: UUID,
    val cteId: String,
    val price: BigDecimal,
    val date: LocalDate?,
    val region: String?,
    val supplier: String?,
    val similarity: Double?,
    val weight: Double?
)
