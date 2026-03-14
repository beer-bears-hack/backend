package tender.hack.domain.dto

import java.time.LocalDate

data class ContractPriceInfo(
    val id: Long,
    val price: Double,
    val date: LocalDate,
    val source: String,
    val region: String?
)
