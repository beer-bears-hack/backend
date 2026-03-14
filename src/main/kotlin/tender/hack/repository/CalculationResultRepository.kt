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
    //TODO: добавить сохранение всех cteId, которые участвовали для этого результата
    fun save(entity: CalculationResultEntity): CalculationResultEntity {
        jdbcClient.sql("""
            INSERT INTO calculation_results (
                user_id, unit_price, total_price, min_price, max_price,
                coeff_variation, is_homogeneous, quantity, method
            ) VALUES (
                :userId, :unitPrice, :totalPrice, :minPrice, :maxPrice,
                :coeffVariation, :isHomogeneous, :quantity, :method
            )
        """.trimIndent())
            .param("userId", entity.sessionId)
            .param("unitPrice", entity.unitPrice)
            .param("totalPrice", entity.totalPrice)
            .param("minPrice", entity.minPrice)
            .param("maxPrice", entity.maxPrice)
            .param("coeffVariation", entity.coeffVariation)
            .param("isHomogeneous", entity.isHomogeneous)
            .param("quantity", entity.quantity)
            .param("method", entity.method)
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
                    method = rs.getString("method")
                )
            }
            .singleOrNull()
    }
}
