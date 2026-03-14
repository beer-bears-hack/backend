package tender.hack.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tender.hack.controller.requests.SearchRequest
import tender.hack.controller.response.SearchResponse
import tender.hack.service.SearchService

/**
 * POST /search
 * Search for comparable items (STE).
 *
 * Request:
 * - query: string
 * - region_code: string (optional)
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Search", description = "Search endpoints")
class SearchController(
    private val searchService: SearchService
) {

    @PostMapping("/search")
    @Operation(summary = "Search for STE items", description = "Returns list of comparable STE items with prices")
    fun search(@RequestBody request: SearchRequest): ResponseEntity<SearchResponse> {
        val results = searchService.search(request.query, request.regionCode)
        return ResponseEntity.ok(SearchResponse(results))
    }
}
