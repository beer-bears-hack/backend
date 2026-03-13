package tender.hack.repository

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import tender.hack.domain.entity.UserEntity
import tender.hack.repository.mapper.UserEntityMapper

@Repository
class UserRepository(
    private val jdbcClient: JdbcClient
) {

    fun createSession(): UserEntity {
        return jdbcClient.sql("""
            insert into users default values
            returning id, created_at;
        """.trimIndent())
            .query(UserEntityMapper)
            .single()
    }
}