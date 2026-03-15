package tender.hack.repository

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import tender.hack.domain.entity.CalculationItemEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
class CalculationItemRepository(
    private val jdbcClient: JdbcClient
) {
    fun save(entity: CalculationItemEntity): CalculationItemEntity {
        jdbcClient.sql("""
            INSERT INTO calculation_items (
                calculation_id, cte_id, price, date, region, supplier, similarity, weight
            ) VALUES (
                :calculationId, :cteId, :price, :date, :region, :supplier, :similarity, :weight
            )
        """.trimIndent())
            .param("calculationId", entity.calculationId)
            .param("cteId", entity.cteId)
            .param("price", entity.price)
            .param("date", entity.date)
            .param("region", entity.region)
            .param("supplier", entity.supplier)
            .param("similarity", entity.similarity)
            .param("weight", entity.weight)
            .update()

        return entity
    }

    fun saveAll(entities: List<CalculationItemEntity>) {
        for (entity in entities) {
            save(entity)
        }
    }

    fun findByCalculationId(calculationId: UUID): List<CalculationItemEntity> {
        return jdbcClient.sql("""
            SELECT * FROM calculation_items WHERE calculation_id = :calculationId
            ORDER BY price ASC
        """.trimIndent())
            .param("calculationId", calculationId)
            .query { rs, _ ->
                CalculationItemEntity(
                    id = rs.getLong("id"),
                    calculationId = UUID.fromString(rs.getString("calculation_id")),
                    cteId = rs.getString("cte_id"),
                    price = rs.getBigDecimal("price"),
                    date = rs.getDate("date")?.toLocalDate(),
                    region = rs.getString("region"),
                    supplier = rs.getString("supplier"),
                    similarity = rs.getDouble("similarity"),
                    weight = rs.getDouble("weight")
                )
            }
            .list()
    }
}
