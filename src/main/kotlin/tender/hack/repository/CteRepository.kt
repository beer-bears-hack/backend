package tender.hack.repository

import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.core.simple.JdbcClient.MappedQuerySpec
import org.springframework.stereotype.Repository
import tender.hack.domain.entity.CteEntity
import tender.hack.repository.mapper.CteEntityMapper

@Repository
class CteRepository(
    private val jdbcClient: JdbcClient
) {
    fun count(): Long {
        return jdbcClient.sql("SELECT COUNT(*) FROM cte")
            .query(Long::class.java)
            .single()
    }

    fun findByCteId(cteId: String): CteEntity? {
        return jdbcClient.sql("""
            select * from cte where cte_id = :cteId
        """.trimIndent())
            .param("cteId", cteId)
            .query(CteEntityMapper)
            .singleOrNull()
    }

    fun findSimilar(cteId: String, limit: Int = 10): List<CteEntity> {
        val sourceCte = findByCteId(cteId) ?: return emptyList()

        return jdbcClient.sql("""
            select * from cte 
            where cte_id != :cteId
            and category = :category
            order by id
            limit :limit
        """.trimIndent())
            .param("cteId", cteId)
            .param("category", sourceCte.category)
            .param("limit", limit)
            .query(CteEntityMapper)
            .list()
    }

    /**
     * Deprecated: currently use ML model
     */
    fun searchByQuery(query: String, limit: Int = 50): List<CteEntity> {
        return jdbcClient.sql("""
            select * from cte 
            where cte_name ilike :query
            or category ilike :query
            or manufacturer ilike :query
            limit :limit
        """.trimIndent())
            .param("query", "%$query%")
            .param("limit", limit)
            .query(CteEntityMapper)
            .list()
    }

    fun saveBatch(ctes: List<CteEntity>) {
        if (ctes.isEmpty()) return

        for (cte in ctes) {
            jdbcClient.sql("""
                INSERT INTO cte (cte_id, cte_name, category, manufacturer, characteristics)
                VALUES (:cteId, :cteName, :category, :manufacturer, :characteristics)
            """.trimIndent())
                .param("cteId", cte.cteId)
                .param("cteName", cte.cteName)
                .param("category", cte.category)
                .param("manufacturer", cte.manufacturer)
                .param("characteristics", cte.characteristics)
                .update()
        }
    }

    fun findAllCategories(): List<String> {
        return jdbcClient.sql("""
            select distinct category from cte
        """.trimIndent())
            .query(String::class.java)
            .list()
    }

    fun findAllManufacturers(): List<String> {
        return jdbcClient.sql("""
            select distinct manufacturer from cte
        """.trimIndent())
            .query(String::class.java)
            .list()
    }

    fun takeCteInfoById(cteId: String): CteEntity? {
        return jdbcClient.sql("""
            select * from cte
            where cte_id = :cte_id
        """.trimIndent())
            .param("cte_id", cteId)
            .query(CteEntityMapper)
            .singleOrNull()
    }
}
