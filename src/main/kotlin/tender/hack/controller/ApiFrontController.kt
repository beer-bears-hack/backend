package tender.hack.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import tender.hack.controller.requests.SearchRequest
import tender.hack.controller.response.SearchResponse
import tender.hack.domain.dto.CteDto
import tender.hack.service.ApiService

@Controller
@RequestMapping("/api")
@Tag(name = "Api methods", description = "Methods in order to get necessary params")
class ApiFrontController(
    private val apiService: ApiService,
) {

    @GetMapping("/categories")
    @Operation(summary = "Find all unique categories")
    fun findCategories(): ResponseEntity<List<String>> {
        val results = apiService.findCategories()
        return ResponseEntity.ok(results)
    }

    @GetMapping("/manufacturers")
    @Operation(summary = "Find all unique manufacturers")
    fun findManufacturers(): ResponseEntity<List<String>> {
        val results = apiService.findManufacturers()
        return ResponseEntity.ok(results)
    }

    @GetMapping("/regions")
    @Operation(summary = "Find all unique regions")
    fun findRegions(): ResponseEntity<List<String>> {
        val results = apiService.findRegions()
        return ResponseEntity.ok(results)
    }

    @GetMapping("/cteInfo/{cteId}")
    @Operation(summary = "Find all info by cteId")
    fun takeCteInfoById(@PathVariable cteId: String): ResponseEntity<CteDto> {
        val result = apiService.takeCteInfoById(cteId)
        return ResponseEntity.ok(result)
    }

}