package tender.hack.domain.entity

import java.math.BigDecimal
import java.time.Instant

data class ContractEntity(
    val id: Long? = null,
    val purchaseName: String,
    val quantity: BigDecimal,
    val contractId: String,
    val purchaseType: String?,
    val initialContractPrice: BigDecimal?,
    val finalContractPrice: BigDecimal?,
    val discountPercent: BigDecimal?,
    val ndsRate: String?,
    val contractDate: Instant?,
    val customerRegion: String?,
    val supplierRegion: String?,
    val cteId: String,
    val cteName: String,
    val unitPrice: BigDecimal,
)
