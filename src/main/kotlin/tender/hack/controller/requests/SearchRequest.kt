package tender.hack.controller.requests

data class SearchRequest(
    val query: String,
    val regionCode: String? = null
)
