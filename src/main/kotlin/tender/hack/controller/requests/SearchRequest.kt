package tender.hack.controller.requests

data class SearchRequest(
    val query: String,
    val category: String? = null,
    val manufacturer: String? = null
)
