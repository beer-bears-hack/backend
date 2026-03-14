package tender.hack.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tender.hack.controller.requests.SearchRequest
import tender.hack.controller.response.SearchResponse
import tender.hack.service.SearchService

@RestController
@RequestMapping("/search")
class SearchController(
    private val searchService: SearchService
) {

    @PostMapping
    fun search(@RequestBody request: SearchRequest): ResponseEntity<SearchResponse> {
        val results = searchService.search(request.query, request.regionCode)
        return ResponseEntity.ok(SearchResponse(results))
    }
}
