package tender.hack.service

import org.apache.poi.xwpf.usermodel.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import tender.hack.domain.entity.CalculationResultEntity
import tender.hack.repository.CalculationResultRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Service for generating justification documents (DOCX format)
 * 
 * Document structure based on "обоснование НМЦК.md" template:
 * - Contract subject
 * - Customer info
 * - Calculation details per position (CTE)
 * - NMCK calculation method description
 * - Final NMCK amount
 * 
 * Yandex Cloud S3 API: https://yandex.cloud/ru/docs/storage/s3/api-ref/hosting/upload
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
            AwsBasicCredentials.create(
                System.getenv("YC_ACCESS_KEY_ID") ?: "",
                System.getenv("YC_SECRET_ACCESS_KEY") ?: ""
            )
        }

        S3Client.builder()
            .region(Region.US_EAST_1)
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
        val signerName: String? = null,
        val purchaseName: String = "Закупка товаров",
        val customerName: String = "Заказчик",
        val customerAddress: String = "Адрес заказчика",
        val customerPhone: String = "+7 (000) 000-00-00",
        val customerEmail: String = "email@example.com",
        val responsiblePerson: String = "Ответственное лицо"
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

    /**
     * Generate DOCX document using Apache POI
     * Follows the template from "обоснование НМЦК.md"
     */
    private fun generateDocxContent(
        result: CalculationResultEntity,
        settings: DocumentSettings
    ): ByteArray {
        val document = XWPFDocument()

        // Title
        createHeading(document, "ОБОСНОВАНИЕ НАЧАЛЬНОЙ МАКСИМАЛЬНОЙ ЦЕНЫ КОНТРАКТА", 16, true)

        // Contract subject
        createParagraph(document, "Предмет контракта:", settings.purchaseName, true)

        // Customer info section
        createSection(document, "Наименование заказчика: ${settings.customerName}")
        createParagraph(document, "Место нахождения:", settings.customerAddress)
        createParagraph(document, "Номер контактного телефона:", settings.customerPhone)
        createParagraph(document, "Адрес электронной почты:", settings.customerEmail)
        createParagraph(document, "Ответственное должностное лицо:", settings.signerName ?: settings.responsiblePerson)

        addEmptyLine(document)

        // Calculation details section
        createHeading(document, "Детализация расчёта по позициям:", 14, true)

        // Position details (CTE)
        createParagraph(document, "Наименование товарной (работной) единицы:", "Позиция ${result.cteId}", true)
        createParagraph(document, "Категория товарной (работной) единицы:", "Категория")
        createParagraph(document, "Производитель товарной (работной) единицы:", "Производитель")
        createParagraph(document, "Характеристики товарной (работной) единицы:", "Характеристики")

        addEmptyLine(document)

        // Justification text
        createParagraph(document, "Обоснование:", "", true)
        createParagraph(document, "", "Расчёт НМЦК для товарной (работной) единицы производился исходя из этих аналогов:")
        
        // Add analogs list (placeholder - would need actual analog data)
        createParagraph(document, "", "1. Аналог 1: ${result.unitPrice} руб.")
        createParagraph(document, "", "2. Аналог 2: ${result.minPrice ?: result.unitPrice} руб.")

        addEmptyLine(document)

        // Price summary
        createParagraph(document, "Стоимость за единицу:", "${result.unitPrice} руб.", true)
        createParagraph(document, "Количество единиц:", "${result.quantity} усл. ед.")
        
        val sum = result.unitPrice.multiply(result.quantity ?: BigDecimal.ONE)
        createParagraph(document, "Сумма по позиции:", "${sum} руб.", true)

        addEmptyLine(document)

        // Total NMCK
        createHeading(document, "Итого НМЦК (без НДС): ${result.totalPrice} руб.", 14, true)

        addEmptyLine(document)

        // Method description
        createHeading(document, "Используемый метод определения НМЦК с обоснованием:", 12, true)
        createParagraph(document, "", 
            "Метод сопоставимых рыночных цен (анализа рынка) в соответствии с частью 6 статьи 22 " +
            "Федерального закона от 05.04.2013 № 44-ФЗ «О контрактной системе в сфере закупок товаров, " +
            "работ, услуг для обеспечения государственных и муниципальных нужд» и Методическими " +
            "рекомендациями, утверждёнными приказом Минэкономразвития России от 02.10.2013 № 567.")

        addEmptyLine(document)

        // Calculation algorithm description
        createHeading(document, "Расчёт НМЦК:", 12, true)
        createParagraph(document, "", 
            "Определение НМЦК произведено на основании анализа ценовой информации из реестра контрактов, " +
            "заключённых заказчиками, с применением автоматизированного алгоритма:")

        // Algorithm steps as bullet list
        val algorithmSteps = listOf(
            "фильтрации контрактов по степени похожести объекта закупки (коэффициент схожесть ≥ X, " +
            "где X = мин(0.8; макс значение схожести – 0.1));",
            "динамического расширения временного окна (3 → 6 → 9–12 месяцев) и региона до достижения " +
            "требуемого эффективного объёма выборки по формуле Киша: n_eff = (Σw_i)² / Σw_i² ≥ 12–15 " +
            "(где w_i = вес давности × вес региона × схожесть);",
            "проверки наличия не менее 3 уникальных поставщиков (при недостатке формируется заключение " +
            "«no data» с указанием причины нехватки данных);",
            "исключения выбросов методом MAD на логарифме цены за единицу " +
            "(x_i = ln(цена за штуку), med = median(x_i), MAD = median(|x_i – med|), " +
            "σ ≈ 1,4826 × MAD, выбросы при |x_i – med| > 3 × σ);",
            "расчёта взвешенной медианы по очищенным данным (получение цены за единицу)."
        )

        for (step in algorithmSteps) {
            createBulletPoint(document, step)
        }

        addEmptyLine(document)

        // Calculation formula
        createHeading(document, "Формула расчёта НМЦК:", 12, true)
        createParagraph(document, "", "НМЦК = цена за единицу × количество единиц (без НДС).")

        addEmptyLine(document)

        // Additional metrics (for internal use)
        if (result.effectiveSampleSize != null) {
            createParagraph(document, "Эффективный размер выборки (KISH):", "${result.effectiveSampleSize}")
        }
        if (result.outliersRemoved != null && result.outliersRemoved > 0) {
            createParagraph(document, "Удалено выбросов:", "${result.outliersRemoved}")
        }
        if (result.similarityThreshold != null) {
            createParagraph(document, "Порог схожести:", "${result.similarityThreshold}")
        }

        addEmptyLine(document)

        // Date
        createParagraph(document, "Дата подготовки обоснования НМЦК:", 
            ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))

        val outputStream = java.io.ByteArrayOutputStream()
        document.write(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    private fun createHeading(document: XWPFDocument, text: String, fontSize: Int = 14, bold: Boolean = true) {
        val paragraph = document.createParagraph()
        paragraph.alignment = ParagraphAlignment.LEFT
        val run = paragraph.createRun()
        run.setText(text)
        run.setBold(bold)
        run.setFontSize(fontSize)
    }

    private fun createSection(document: XWPFDocument, text: String) {
        val paragraph = document.createParagraph()
        paragraph.alignment = ParagraphAlignment.LEFT
        val run = paragraph.createRun()
        run.setText(text)
        run.setBold(true)
        run.setFontSize(12)
    }

    private fun createParagraph(document: XWPFDocument, label: String, value: String, bold: Boolean = false) {
        val paragraph = document.createParagraph()
        paragraph.alignment = ParagraphAlignment.LEFT
        val run = paragraph.createRun()
        if (label.isNotEmpty()) {
            run.setText("$label $value")
            run.setBold(bold)
        } else {
            run.setText(value)
        }
        run.setFontSize(11)
    }

    private fun createBulletPoint(document: XWPFDocument, text: String) {
        val paragraph = document.createParagraph()
        paragraph.alignment = ParagraphAlignment.LEFT
        val run = paragraph.createRun()
        run.setText("• $text")
        run.setFontSize(11)
    }

    private fun addEmptyLine(document: XWPFDocument) {
        document.createParagraph()
    }

    /**
     * Upload file to Yandex Cloud S3
     * 
     * According to Yandex Cloud docs:
     * - PUT request to https://storage.yandexcloud.net/{bucket}/{key}
     * - AWS Signature v4 authentication required
     * - Content-Type header should be set
     * - Content-MD5 header must be Base64 encoded (not hex!)
     */
    private fun uploadToS3(fileName: String, content: ByteArray): String {
        val key = "documents/$fileName"

        try {
            // Calculate MD5 and encode to Base64 (S3 expects Base64, not hex string)
            val md5Hash = java.security.MessageDigest.getInstance("MD5")
                .digest(content)
                .let { java.util.Base64.getEncoder().encodeToString(it) }

            val putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .contentMD5(md5Hash)
                .build()

            s3Client.putObject(putRequest, RequestBody.fromBytes(content))

            return "https://storage.yandexcloud.net/$bucketName/$key"
        } catch (e: Exception) {
            log.error("Failed to upload to S3: ${e.message}", e)
            return "/api/documents/download/$fileName"
        }
    }
}
