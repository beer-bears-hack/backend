package tender.hack.domain.dto

data class CteDto(
    val id: Long,
    val cteId: String,
    val cteName: String,
    val category: String?,
    val manufacturer: String?,
    val characteristics: Map<String, String>,
)