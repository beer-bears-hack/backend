package tender.hack.repository.mapper

import org.springframework.jdbc.core.RowMapper
import tender.hack.domain.entity.CteEntity
import java.sql.ResultSet

object CteEntityMapper : RowMapper<CteEntity> {
    override fun mapRow(rs: ResultSet, rowNum: Int): CteEntity? {
        return CteEntity(
            id = rs.getLong("id"),
            cteId = rs.getString("cte_id"),
            cteName = rs.getString("cte_name"),
            category = rs.getString("category"),
            manufacturer = rs.getString("manufacturer"),
            characteristics = rs.getString("characteristics"),
        )
    }
}
