package tender.hack.domain.entity

import java.util.UUID
import java.time.OffsetDateTime

class UserEntity(
    val uuid: UUID,
    val created: OffsetDateTime
)