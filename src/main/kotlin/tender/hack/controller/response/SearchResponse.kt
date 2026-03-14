package tender.hack.controller.response

data class SearchResponse(
    val results: List<SearchResultItem>
)

data class SearchResultItem(
    val steId: String,
    val name: String,
    val characteristics: Map<String, String>,
    val similarityScore: Double,
    val category: String,
    val kpgzCode: String? = null,
    val kpgzName: String? = null
)
