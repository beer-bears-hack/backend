package tender.hack.repository.mapper

import org.springframework.jdbc.core.RowMapper
import tender.hack.domain.entity.UserEntity
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.*


object UserEntityMapper: RowMapper<UserEntity> {

    override fun mapRow(rs: ResultSet, rowNum: Int): UserEntity {
        val id: UUID = rs.getObject("id", UUID::class.java)
        val createdAt: OffsetDateTime = rs.getObject("created_at", OffsetDateTime::class.java)
        return UserEntity(id, createdAt)
    }
}