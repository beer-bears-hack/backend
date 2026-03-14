package tender.hack.domain.dto

import java.time.LocalDate

class PriceDto(
    val id: Long,
    val price: Double,
    val date: LocalDate,
    val source: String,
    val isOutlier: Boolean = false,
    val reason: String? = null,
)
