package tender.hack.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tender.hack.controller.response.SearchResultItem
import tender.hack.domain.dto.PriceDto
import tender.hack.repository.CteRepository
import tender.hack.repository.ContractRepository
import java.util.concurrent.TimeUnit

@Service
class SearchService(
    private val cteRepository: CteRepository,
    private val contractRepository: ContractRepository,
    @Value("\${ml.api.url}")
    private val mlApiUrl: String
) {
    private val log = LoggerFactory.getLogger(SearchService::class.java)
    private val objectMapper = jacksonObjectMapper()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Search for similar items using ML model
     * 
     * ML API:
     * POST /search
     * Request: {"query": "Наименование: ... Категория: ... Характеристики: ..."}
     * Response: {"results": [{"cte_id": 123, "score": 0.95}, ...]}
     */
    fun search(query: String, category: String?, manufacturer: String?): List<SearchResultItem> {
        log.info("Search request: query='$query', category=$category, manufacturer=$manufacturer)")

        val categoryAfterCheck = category ?: ""
        val manufacturerAfterCheck = manufacturer ?: ""

        // Call ML API to get similar CTE IDs
//        val queryForMl = "Наименование: '$query'. Категория: '$categoryAfterCheck'. Производитель: '$manufacturerAfterCheck'."
        val queryForMl = "'$query''$categoryAfterCheck''$manufacturerAfterCheck''"
        val mlResponse = callMlSearch(queryForMl)
        
        // Get full CTE data for each result only for ones, where score more than predefined limit
        return mlResponse.results.map { result ->
            val cte = cteRepository.findByCteId(result.cteId.toString())
            
            val prices = if (cte != null) {
                val contractPrices = contractRepository.findByCteId(result.cteId.toString())
                contractPrices.map { contract ->
                    PriceDto(
                        contractId = contract.contractId,
                        price = contract.price,
                        date = contract.date,
                        source = contract.source ?: "Unknown",
                        isOutlier = false,
                        reason = null
                    )
                }
            } else {
                emptyList()
            }

            SearchResultItem(
                cteId = result.cteId.toString(),
                name = cte?.cteName ?: "Unknown",
                characteristics = parseCharacteristics(cte?.characteristics),
                similarityScore = result.score,
                category = cte?.category ?: "Unknown",
                kpgzCode = null,
                kpgzName = null,
                prices = prices
            )
        }
    }

    /**
     * Call ML search API using OkHttp
     * 
     * Query format (matching Python build_item_text):
     * "Наименование: {name}. Категория: {category}. Производитель: {manufacturer}. Характеристики: {characteristics}"
     */
    private fun callMlSearch(query: String): MlSearchResponse {
        val json = objectMapper.writeValueAsString(mapOf("query" to query, "top_k" to 100))
        
        val request = Request.Builder()
            .url("$mlApiUrl/search")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                log.error("ML API request failed: ${response.code}")
                return MlSearchResponse(emptyList())
            }
            
            val responseBody = response.body?.string() ?: return MlSearchResponse(emptyList())
            objectMapper.readValue<MlSearchResponse>(responseBody)
        }
    }

    private fun parseCharacteristics(characteristics: String?): Map<String, String> {
        if (characteristics.isNullOrBlank()) return emptyMap()

        return characteristics.split(";")
            .mapNotNull { part ->
                val kv = part.split(":")
                if (kv.size == 2) kv[0].trim() to kv[1].trim() else null
            }
            .toMap()
    }
}

/**
 * ML API Request/Response DTOs
 */
data class MlSearchResponse(
    val results: List<MlSearchResult>
)

data class MlSearchResult(
    @JsonProperty("cte_id") val cteId: Long,
    val score: Double
)
