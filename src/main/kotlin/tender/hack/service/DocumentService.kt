package tender.hack.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import tender.hack.repository.CalculationResultRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Service for generating justification documents
 * 
 * Yandex Cloud S3 API: https://yandex.cloud/ru/docs/storage/s3/api-ref/hosting/upload
 * 
 * Requirements:
 * - AWS Signature Version 4 authentication
 * - Endpoint: https://storage.yandexcloud.net
 * - Access key and secret key from Yandex Cloud IAM
 */
@Service
class DocumentService(
    private val calculationResultRepository: CalculationResultRepository
) {
    private val log = LoggerFactory.getLogger(DocumentService::class.java)

    @Value("\${yandex.cloud.bucket:hackathon-perm-2026}")
    private val bucketName: String = "hackathon-perm-2026"

    @Value("\${yandex.cloud.access-key-id:}")
    private val accessKeyId: String = ""

    @Value("\${yandex.cloud.secret-access-key:}")
    private val secretAccessKey: String = ""

    private val s3Client: S3Client by lazy {
        val creds = if (accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()) {
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        } else {
            // Fallback to environment variables
            AwsBasicCredentials.create(
                System.getenv("YC_ACCESS_KEY_ID") ?: "",
                System.getenv("YC_SECRET_ACCESS_KEY") ?: ""
            )
        }

        S3Client.builder()
            .region(Region.US_EAST_1) // Yandex Cloud uses US_EAST_1 for all buckets
            .endpointOverride(java.net.URI("https://storage.yandexcloud.net"))
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .build()
    }

    data class GenerateDocumentRequest(
        val sessionId: String,
        val settings: DocumentSettings
    )

    data class DocumentSettings(
        val includeCoverPage: Boolean = true,
        val signerName: String? = null
    )

    data class GenerateDocumentResponse(
        val fileUrl: String,
        val generatedAt: String
    )

    fun generateDocument(request: GenerateDocumentRequest): GenerateDocumentResponse {
        log.info("Generate document for session: ${request.sessionId}")

        val sessionId = UUID.fromString(request.sessionId)
        val calculationResult = calculationResultRepository.findBySessionId(sessionId)
            ?: throw IllegalArgumentException("No calculation result found for session: $sessionId")

        // Generate DOCX content
        val docxContent = generateDocxContent(calculationResult, request.settings)

        // Upload to S3
        val fileName = "justification_${sessionId}_${System.currentTimeMillis()}.docx"
        val fileUrl = uploadToS3(fileName, docxContent)

        val generatedAt = Instant.now()
            .atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)

        return GenerateDocumentResponse(
            fileUrl = fileUrl,
            generatedAt = generatedAt
        )
    }

    private fun generateDocxContent(
        result: tender.hack.domain.entity.CalculationResultEntity,
        settings: DocumentSettings
    ): ByteArray {
        // TODO: Generate real DOCX using Apache POI
        // For now, return a simple text placeholder

        val content = buildString {
            if (settings.includeCoverPage) {
                appendLine("ОБОСНОВАНИЕ НАЧАЛЬНОЙ МАКСИМАЛЬНОЙ ЦЕНЫ КОНТРАКТА")
                appendLine("")
                appendLine("========================================")
                appendLine("")
            }

            appendLine("Расчёт НМЦК")
            appendLine("")
            appendLine("Единица измерения: шт.")
            appendLine("Количество: ${result.quantity}")
            appendLine("")
            appendLine("Результаты расчёта:")
            appendLine("- Цена за единицу: ${result.unitPrice} руб.")
            appendLine("- Общая сумма: ${result.totalPrice} руб.")
            appendLine("- Минимальная цена: ${result.minPrice} руб.")
            appendLine("- Максимальная цена: ${result.maxPrice} руб.")
            appendLine("- Коэффициент вариации: ${result.coeffVariation}")
            appendLine("- Однородность: ${if (result.isHomogeneous) "Да" else "Нет"}")
            appendLine("")

            if (settings.signerName != null) {
                appendLine("Подписант: ${settings.signerName}")
            }

            appendLine("")
            appendLine("Дата формирования: ${Instant.now()}")
        }

        return content.toByteArray()
    }

    /**
     * Upload file to Yandex Cloud S3
     * 
     * According to Yandex Cloud docs:
     * - PUT request to https://storage.yandexcloud.net/{bucket}/{key}
     * - AWS Signature v4 authentication required
     * - Content-Type header should be set
     * - Content-MD5 header recommended
     */
    private fun uploadToS3(fileName: String, content: ByteArray): String {
        val key = "documents/$fileName"

        try {
            // Calculate MD5 for Content-MD5 header (required by Yandex Cloud)
            val md5Hash = java.security.MessageDigest.getInstance("MD5")
                .digest(content)
                .let { it.joinToString("") { "%02x".format(it) } }

            val putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .contentMD5(md5Hash)
                .build()

            s3Client.putObject(putRequest, RequestBody.fromBytes(content))

            // Return public URL
            return "https://storage.yandexcloud.net/$bucketName/$key"
        } catch (e: Exception) {
            log.error("Failed to upload to S3: ${e.message}", e)
            // Return local URL for development without S3 credentials
            return "/api/documents/download/$fileName"
        }
    }
}
