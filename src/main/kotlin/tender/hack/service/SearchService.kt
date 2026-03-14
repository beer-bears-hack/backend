package tender.hack.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tender.hack.controller.response.SearchResultItem
import tender.hack.controller.response.SearchResponse
import tender.hack.domain.dto.PriceDto
import tender.hack.repository.CteRepository
import tender.hack.repository.ContractRepository

@Service
class SearchService(
    private val cteRepository: CteRepository,
    private val contractRepository: ContractRepository
) {
    private val log = LoggerFactory.getLogger(SearchService::class.java)

    fun search(query: String, regionCode: String?): List<SearchResultItem> {
        log.info("Search request: query='$query', regionCode=$regionCode")

        // TODO: Здесь будет вызов ML модели для поиска похожих товаров
        // Пока возвращаем дефолтные значения на основе поиска по названию в CTE

        val cteResults = cteRepository.searchByQuery(query, limit = 20)

        return cteResults.map { cte ->
            //TODO: count prices in PriceService and compute isOutlier by formula
            val contractPrices = contractRepository.findByCteId(cte.cteId)
            val prices = contractPrices.map { contract ->
                PriceDto(
                    id = contract.id,
                    price = contract.price,
                    date = contract.date,
                    source = contract.source ?: "Unknown",
                    isOutlier = false, // TODO: ML model should detect outliers
                    reason = null // TODO: ML model should provide reason for outliers
                )
            }

            SearchResultItem(
                steId = cte.cteId,
                name = cte.cteName,
                characteristics = parseCharacteristics(cte.characteristics),
                similarityScore = 0.85, // TODO: ML model should calculate this
                category = cte.category ?: "Unknown",
                kpgzCode = null, // TODO: ML model should provide this
                kpgzName = null, // TODO: ML model should provide this
                prices = prices
            )
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
