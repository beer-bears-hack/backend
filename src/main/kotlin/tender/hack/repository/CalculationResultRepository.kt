package tender.hack.repository

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import tender.hack.domain.entity.CalculationResultEntity
import java.math.BigDecimal
import java.util.UUID

@Repository
class CalculationResultRepository(
    private val jdbcClient: JdbcClient
) {
    fun save(entity: CalculationResultEntity): CalculationResultEntity {
        jdbcClient.sql("""
            INSERT INTO calculation_results (
                id, user_id, unit_price, total_price, min_price, max_price,
                coeff_variation, is_homogeneous, quantity, method, cte_id,
                effective_sample_size, outliers_removed, similarity_threshold, no_data_reason
            ) VALUES (
                :id, :userId, :unitPrice, :totalPrice, :minPrice, :maxPrice,
                :coeffVariation, :isHomogeneous, :quantity, :method, :cte_id,
                :effectiveSampleSize, :outliersRemoved, :similarityThreshold, :noDataReason
            )
        """.trimIndent())
            .param("id", entity.id)
            .param("userId", entity.sessionId)
            .param("unitPrice", entity.unitPrice)
            .param("totalPrice", entity.totalPrice)
            .param("minPrice", entity.minPrice)
            .param("maxPrice", entity.maxPrice)
            .param("coeffVariation", entity.coeffVariation)
            .param("isHomogeneous", entity.isHomogeneous)
            .param("quantity", entity.quantity)
            .param("method", entity.method)
            .param("cte_id", entity.cteId)
            .param("effectiveSampleSize", entity.effectiveSampleSize)
            .param("outliersRemoved", entity.outliersRemoved)
            .param("similarityThreshold", entity.similarityThreshold)
            .param("noDataReason", entity.noDataReason)
            .update()

        return entity
    }

    fun findBySessionId(sessionId: UUID): CalculationResultEntity? {
        return jdbcClient.sql("""
            SELECT * FROM calculation_results WHERE user_id = :userId
            ORDER BY created_at DESC LIMIT 1
        """.trimIndent())
            .param("userId", sessionId)
            .query { rs, _ ->
                CalculationResultEntity(
                    id = UUID.fromString(rs.getString("id")),
                    sessionId = sessionId,
                    unitPrice = rs.getBigDecimal("unit_price"),
                    totalPrice = rs.getBigDecimal("total_price"),
                    minPrice = rs.getBigDecimal("min_price"),
                    maxPrice = rs.getBigDecimal("max_price"),
                    coeffVariation = rs.getDouble("coeff_variation"),
                    isHomogeneous = rs.getBoolean("is_homogeneous"),
                    quantity = rs.getBigDecimal("quantity"),
                    method = rs.getString("method"),
                    cteId = rs.getString("cte_id"),
                    effectiveSampleSize = rs.getDouble("effective_sample_size"),
                    outliersRemoved = rs.getInt("outliers_removed"),
                    similarityThreshold = rs.getDouble("similarity_threshold"),
                    noDataReason = rs.getString("no_data_reason")
                )
            }
            .singleOrNull()
    }

    fun findResultsBySessionId(sessionId: UUID): List<CalculationResultEntity> {
        return jdbcClient.sql("""
            SELECT * FROM calculation_results WHERE user_id = :userId
            ORDER BY created_at DESC
        """.trimIndent())
            .param("userId", sessionId)
            .query { rs, _ ->
                CalculationResultEntity(
                    id = UUID.fromString(rs.getString("id")),
                    sessionId = sessionId,
                    unitPrice = rs.getBigDecimal("unit_price"),
                    totalPrice = rs.getBigDecimal("total_price"),
                    minPrice = rs.getBigDecimal("min_price"),
                    maxPrice = rs.getBigDecimal("max_price"),
                    coeffVariation = rs.getDouble("coeff_variation"),
                    isHomogeneous = rs.getBoolean("is_homogeneous"),
                    quantity = rs.getBigDecimal("quantity"),
                    method = rs.getString("method"),
                    cteId = rs.getString("cte_id"),
                    effectiveSampleSize = rs.getDouble("effective_sample_size"),
                    outliersRemoved = rs.getInt("outliers_removed"),
                    similarityThreshold = rs.getDouble("similarity_threshold"),
                    noDataReason = rs.getString("no_data_reason")
                )
            }
            .list()
    }

    fun deleteResultByCteId(sessionId: UUID, id: UUID) {
        jdbcClient.sql("""
            delete from calculation_results 
            where user_id = :userId and id = :id
        """.trimIndent())
            .param("userId", sessionId)
            .param("id", id)
            .update()
    }
}
