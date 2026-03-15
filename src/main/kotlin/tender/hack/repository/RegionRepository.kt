package tender.hack.repository

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import tender.hack.domain.entity.RegionEntity

@Repository
class RegionRepository(
    private val jdbcClient: JdbcClient
) {
    fun findByName(regionName: String): RegionEntity? {
        val sql = """
            SELECT id, region, lat, lon FROM regions WHERE region = :region
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("region", regionName)
            .query { rs, _ ->
                RegionEntity(
                    id = rs.getLong("id"),
                    region = rs.getString("region"),
                    lat = rs.getDouble("lat"),
                    lon = rs.getDouble("lon")
                )
            }
            .singleOrNull()
    }

    fun findAll(): List<RegionEntity> {
        val sql = "SELECT id, region, lat, lon FROM regions"

        return jdbcClient.sql(sql)
            .query { rs, _ ->
                RegionEntity(
                    id = rs.getLong("id"),
                    region = rs.getString("region"),
                    lat = rs.getDouble("lat"),
                    lon = rs.getDouble("lon")
                )
            }
            .list()
    }

    fun findAllRegions(): List<String> {
        return jdbcClient.sql("""
            select region from regions
        """.trimIndent())
            .query(String::class.java)
            .list()
    }
}
