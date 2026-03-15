package tender.hack.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tender.hack.controller.requests.CalculateItemRequest
import tender.hack.controller.requests.SaveCalculationRequest
import tender.hack.controller.response.CalculateItemResponse
import tender.hack.controller.response.PriceRange
import tender.hack.domain.dto.ContractPriceInfo
import tender.hack.domain.entity.CalculationResultEntity
import tender.hack.domain.entity.RegionEntity
import tender.hack.repository.CalculationResultRepository
import tender.hack.repository.ContractRepository
import tender.hack.repository.RegionRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.*

/**
 * Service for calculating NMCK (initial procurement price)
 *
 * Algorithm:
 * 1. Filter CTE by similarity (X = min(0.8, BEST-0.1))
 * 2. Search window for enough data (3→6→9→12 months, expand region)
 * 3. Use KISH effective sample size (neff = (Σwi)² / Σ(wi²))
 * 4. Remove outliers using MAD on log-prices
 * 5. Calculate weighted median
 *
 * Weights: w_i = weight_date * weight_region * similarity
 */
@Service
class CalculationService(
    private val contractRepository: ContractRepository,
    private val calculationResultRepository: CalculationResultRepository,
    private val regionRepository: RegionRepository
) {
    private val log = LoggerFactory.getLogger(CalculationService::class.java)

    // Constants for weight calculation
    private val DATE_DECAY_STEP = 0.05  // Linear decay per month for first 6 months
    private val DATE_EXPONENTIAL_DECAY = 0.85  // Exponential decay factor after 6 months
    private val REGION_HALF_LIFE_KM = 500.0  // Distance for half weight
    private val EARTH_RADIUS_KM = 6371.0

    // Constants for sampling
    private val SIMILARITY_THRESHOLD_BASE = 0.8
    private val SIMILARITY_THRESHOLD_MARGIN = 0.1
    private val MIN_EFFECTIVE_SAMPLE_SIZE = 12.0
    private val MIN_UNIQUE_SUPPLIERS = 3

    // Constants for outlier detection
    private val MAD_K_THRESHOLD = 3.0  // Number of sigmas for outlier

    fun calculate(request: CalculateItemRequest): CalculateItemResponse {
        log.info("Calculate request: ${request.items.size} items, region=${request.region}")

        // Step 1: Collect all price data with metadata
        //TODO check unique amount of manufacturers for all ctes
        val priceDataList = collectPriceData(request)

        if (priceDataList.isEmpty()) {
            return createNoDataResponse(request.quantity, "Недостаточно данных для анализа")
        }

        // Step 2: Filter by similarity (for ML results)
        val filteredBySimilarity = filterBySimilarity(priceDataList)

        if (filteredBySimilarity.isEmpty()) {
            return createNoDataResponse(request.quantity, "Нет данных с достаточной схожестью")
        }

        // Step 3: Search for adequate window (time + region)
        val windowResult = findAdequateWindow(filteredBySimilarity, request.region)

        if (windowResult.data.isEmpty()) {
            return createNoDataResponse(request.quantity, windowResult.reason ?: "Недостаточно данных")
        }

        // Step 4: Calculate weights for each data point
        val weightedData = windowResult.data.map {
            it.copy(weight = calculateWeight(it, request.region))
        }

        // Step 5: Check effective sample size (KISH)
        val neff = calculateKishNeff(weightedData)
        log.info("Effective sample size (KISH): $neff")

        if (neff < MIN_EFFECTIVE_SAMPLE_SIZE) {
            return createNoDataResponse(request.quantity, "Недостаточный размер выборки (neff=$neff)")
        }

        // Step 6: Remove outliers using MAD on log-prices
        val cleanedData = removeOutliersMAD(weightedData)

        if (cleanedData.size < MIN_UNIQUE_SUPPLIERS) {
            return createNoDataResponse(request.quantity, "Недостаточно поставщиков после удаления выбросов")
        }

        // Step 7: Calculate weighted median
        val weightedMedianPrice = calculateWeightedMedian(cleanedData)

        // Step 8: Calculate statistics
        val prices = cleanedData.map { it.price }
        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 0.0

        val unitPrice = weightedMedianPrice
        val totalPrice = unitPrice * request.quantity

        // Calculate coefficient of variation for homogeneity check
        val stdDev = calculateStandardDeviation(prices)
        val avgPrice = prices.average()
        val coeffVariation = if (avgPrice > 0) stdDev / avgPrice else 0.0
        val isHomogeneous = coeffVariation < 0.33

        log.info("Calculation complete: unitPrice=$unitPrice, neff=$neff, outliers_removed=${weightedData.size - cleanedData.size}")

        return CalculateItemResponse(
            unitPrice = unitPrice,
            totalPrice = totalPrice,
            priceRange = PriceRange(minPrice, maxPrice),
            coeffVariation = coeffVariation,
            isHomogeneous = isHomogeneous,
            quantity = request.quantity,
            effectiveSampleSize = neff,
            outliersRemoved = weightedData.size - cleanedData.size
        )
    }

    /**
     * Collect all price data from contracts
     * 
     * Logic:
     * - contractId != null: Fetch from DB (ML/contract results), use similarity from item
     * - contractId == null && price != null: Manual price entry, similarity = 1.0
     * - source field: Contains reason/description for manual entries (not supplier name)
     */
    private fun collectPriceData(request: CalculateItemRequest): List<PriceDataPoint> {
        val result = mutableListOf<PriceDataPoint>()

        for (item in request.items) {
            if (item.contractId != null) {
                // ML/Contract results - fetch prices from database
                val contractPrices = contractRepository.findByContractIdAndCteId(item.contractId, item.cteId)
                result.addAll(contractPrices.map { cp ->
                    PriceDataPoint(
                        price = cp.price,
                        date = cp.date,
                        region = cp.region ?: request.region,
                        supplier = cp.source,  // Actual supplier name from contract
                        similarity = item.similarity,  // Use ML similarity score
                        cteId = item.cteId
                    )
                })
            } else if (item.price != null) {
                // Manual price entry - use provided price directly
                result.add(
                    PriceDataPoint(
                        price = item.price,
                        date = LocalDate.now(),
                        region = request.region,
                        supplier = "manual",  // Mark as manual entry
                        similarity = 1.0,  // Full similarity for manual entries
                        cteId = item.cteId
                    )
                )
            }
        }

        return result
    }

    /**
     * Filter by similarity threshold
     * X = min(0.8, BEST-0.1) where BEST is the highest similarity score
     */
    private fun filterBySimilarity(data: List<PriceDataPoint>): List<PriceDataPoint> {
        if (data.isEmpty()) return emptyList()

        val bestSimilarity = data.maxOf { it.similarity }
        val threshold = min(SIMILARITY_THRESHOLD_BASE, bestSimilarity - SIMILARITY_THRESHOLD_MARGIN)

        log.info("Similarity filter: best=$bestSimilarity, threshold=$threshold")

        return data.filter { it.similarity >= threshold }
    }

    /**
     * Find adequate time window with enough data
     * Try: 3 months → 6 months → 9 months → 12 months → expand region
     */
    private fun findAdequateWindow(
        data: List<PriceDataPoint>,
        targetRegion: String?
    ): WindowResult {
        val now = LocalDate.now()

        // Try different time windows
        val windows = listOf(3, 6, 9, 12)

        for (months in windows) {
            val cutoffDate = now.minusMonths(months.toLong())
            val windowData = data.filter { it.date.isAfter(cutoffDate) }

            val neff = calculateKishNeff(windowData.map { it.copy(weight = 1.0) })
            val uniqueSuppliers = windowData.map { it.supplier }.toSet().size

            log.info("Window ${months} months: ${windowData.size} points, neff=$neff, suppliers=$uniqueSuppliers")

            if (neff >= MIN_EFFECTIVE_SAMPLE_SIZE && uniqueSuppliers >= MIN_UNIQUE_SUPPLIERS) {
                return WindowResult(windowData, null)
            }
        }

        // If still not enough, try expanding region (for now, just return 12-month data)
        val yearData = data.filter { it.date.isAfter(now.minusMonths(12)) }
        if (yearData.isNotEmpty()) {
            return WindowResult(yearData, "Расширенное окно (12 месяцев)")
        }

        return WindowResult(emptyList(), "Недостаточно данных даже за 12 месяцев")
    }

    /**
     * Calculate weight for a data point
     * w_i = weight_date * weight_region * similarity
     */
    private fun calculateWeight(data: PriceDataPoint, targetRegion: String): Double {
        val weightDate = calculateDateWeight(data.date)
        val weightRegion = calculateRegionWeight(data.region, targetRegion)

        return weightDate * weightRegion * data.similarity
    }

    /**
     * Calculate date weight
     * - First 6 months: linear decay with step 0.05 per month
     * - After 6 months: exponential decay
     */
    private fun calculateDateWeight(date: LocalDate): Double {
        val monthsAgo = ChronoUnit.MONTHS.between(date, LocalDate.now()).coerceAtLeast(0).toInt()

        return if (monthsAgo <= 6) {
            // Linear decay: 1.0 at 0 months, 0.75 at 5 months, 0.70 at 6 months
            max(0.0, 1.0 - monthsAgo * DATE_DECAY_STEP)
        } else {
            // Exponential decay after 6 months
            val monthsAfterSix = monthsAgo - 6
            0.70 * DATE_EXPONENTIAL_DECAY.pow(monthsAfterSix)
        }
    }

    /**
     * Calculate region weight based on distance
     * Half-life decay: weight = 0.5^(distance / halfLife)
     */
    private fun calculateRegionWeight(dataRegion: String?, targetRegion: String): Double {
        // If no region info or same region, return full weight
        if (dataRegion == null) return 1.0
        if (dataRegion == targetRegion) return 1.0

        // Calculate distance between regions
        val distance = calculateRegionDistance(dataRegion, targetRegion)

        // Half-life decay: weight = 2^(-distance / halfLife)
        return 2.0.pow(-distance / REGION_HALF_LIFE_KM)
    }

    /**
     * Calculate distance between two regions using Haversine formula
     */
    private fun calculateRegionDistance(region1: String, region2: String): Double {
        val r1 = regionRepository.findByName(region1)
        val r2 = regionRepository.findByName(region2)

        if (r1 == null || r2 == null) return REGION_HALF_LIFE_KM * 2  // Default large distance

        return haversineDistance(r1.lat, r1.lon, r2.lat, r2.lon)
    }

    /**
     * Haversine formula for distance between two points on Earth
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * KISH Effective Sample Size
     * neff = (Σwi)² / Σ(wi²)
     */
    private fun calculateKishNeff(weightedData: List<PriceDataPoint>): Double {
        if (weightedData.isEmpty()) return 0.0

        val sumWeights = weightedData.sumOf { it.weight }
        val sumSquaredWeights = weightedData.sumOf { it.weight * it.weight }

        if (sumSquaredWeights == 0.0) return 0.0

        return (sumWeights * sumWeights) / sumSquaredWeights
    }

    /**
     * Remove outliers using MAD (Median Absolute Deviation) on log-prices
     */
    private fun removeOutliersMAD(data: List<PriceDataPoint>): List<PriceDataPoint> {
        if (data.size < 3) return data

        // Convert to log-prices
        val logPrices = data.map { ln(it.price) }

        // Calculate median of log-prices
        val sortedLogPrices = logPrices.sorted()
        val median = sortedLogPrices[sortedLogPrices.size / 2]

        // Calculate MAD
        val absDeviations = logPrices.map { abs(it - median) }
        val sortedDeviations = absDeviations.sorted()
        val mad = sortedDeviations[sortedDeviations.size / 2]

        // Robust sigma estimate
        val sigma = 1.4826 * mad

        if (sigma == 0.0) return data  // No variation

        // Filter outliers: |x_i - med| <= k * sigma
        val threshold = MAD_K_THRESHOLD * sigma

        return data.filterIndexed { index, _ ->
            abs(logPrices[index] - median) <= threshold
        }
    }

    /**
     * Calculate weighted median
     */
    private fun calculateWeightedMedian(data: List<PriceDataPoint>): Double {
        if (data.isEmpty()) return 0.0

        // Sort by price
        val sorted = data.sortedBy { it.price }

        // Calculate total weight
        val totalWeight = sorted.sumOf { it.weight }

        // Find median (where cumulative weight reaches 50%)
        var cumulativeWeight = 0.0
        for (point in sorted) {
            cumulativeWeight += point.weight
            if (cumulativeWeight >= totalWeight / 2) {
                return point.price
            }
        }

        return sorted.last().price
    }

    private fun createNoDataResponse(quantity: Int, reason: String): CalculateItemResponse {
        log.warn("No data: $reason")
        return CalculateItemResponse(
            unitPrice = 0.0,
            totalPrice = 0.0,
            priceRange = PriceRange(0.0, 0.0),
            coeffVariation = 0.0,
            isHomogeneous = true,
            quantity = quantity,
            effectiveSampleSize = 0.0,
            outliersRemoved = 0,
            noDataReason = reason
        )
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0

        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }

    fun saveCalculation(sessionId: UUID, request: SaveCalculationRequest) {
        // Calculate similarity threshold: X = min(0.8, BEST-0.1)
        // For saved results, we use a default based on effective sample size
        val similarityThreshold = if (request.effectiveSampleSize > 0) {
            min(0.8, 1.0 - 0.1)  // Assuming best similarity is 1.0 for saved data
        } else {
            null
        }

        val entity = CalculationResultEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            unitPrice = BigDecimal.valueOf(request.unitPrice),
            totalPrice = BigDecimal.valueOf(request.totalPrice),
            minPrice = BigDecimal.valueOf(request.priceRange.min),
            maxPrice = BigDecimal.valueOf(request.priceRange.max),
            coeffVariation = request.coeffVariation,
            isHomogeneous = request.isHomogeneous,
            quantity = BigDecimal.valueOf(request.quantity.toLong()),
            method = null,
            cteId = request.cteId,
            effectiveSampleSize = request.effectiveSampleSize,
            outliersRemoved = request.outliersRemoved,
            similarityThreshold = similarityThreshold,
            noDataReason = request.noDataReason
        )
        calculationResultRepository.save(entity)
        log.info("Saved calculation result for session $sessionId")
    }

    // Data classes for calculation
    data class PriceDataPoint(
        val price: Double,
        val date: LocalDate,
        val region: String,
        val supplier: String,
        val similarity: Double,
        val cteId: String,
        val weight: Double = 1.0
    )

    data class WindowResult(
        val data: List<PriceDataPoint>,
        val reason: String?
    )
}
