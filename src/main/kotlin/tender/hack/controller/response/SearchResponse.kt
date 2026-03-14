package tender.hack.controller.response

import tender.hack.domain.dto.PriceDto

data class SearchResponse(
    val results: List<SearchResultItem>
)

data class SearchResultItem(
    val cteId: String,
    val name: String,
    val characteristics: Map<String, String>,
    val similarityScore: Double,
    val category: String,
    val kpgzCode: String? = null,
    val kpgzName: String? = null,
    val prices: List<PriceDto> = emptyList()
)
