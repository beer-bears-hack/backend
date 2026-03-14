package tender.hack.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tender.hack.service.DocumentService

/**
 * POST /documents/generate
 * Generate DOCX justification document.
 *
 * Request:
 * - session_id: string
 * - settings: { include_cover_page: boolean, signer_name: string }
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Documents", description = "Document generation endpoints")
class DocumentController(
    private val documentService: DocumentService
) {

    @PostMapping("/documents/generate")
    @Operation(summary = "Generate justification document", description = "Generate DOCX document with price calculation justification")
    fun generateDocument(@RequestBody request: DocumentService.GenerateDocumentRequest): ResponseEntity<DocumentService.GenerateDocumentResponse> {
        val response = documentService.generateDocument(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/documents/download/{filename}")
    @Operation(summary = "Download generated document", description = "Download the generated DOCX file")
    fun downloadDocument(@PathVariable filename: String): ResponseEntity<ByteArray> {
        // TODO: Implement download from S3
        return ResponseEntity.notFound().build()
    }
}
